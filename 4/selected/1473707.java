package touchstone;

import DDSTouchStone.*;
import ddshelp.*;
import DDS.*;
import java.util.*;

public class Transceiver {

    private static class Metrics {

        private final ArrayList<Double> m_stamps = new ArrayList<Double>(10000);

        public synchronized void init() {
            m_stamps.clear();
        }

        public synchronized void add(double stamp) {
            m_stamps.add(stamp);
        }

        public synchronized void analyze(metricsReport[] reports) {
            assert (reports.length == 4);
            Collections.sort(m_stamps);
            report(100.0, reports[0]);
            report(99.9, reports[1]);
            report(99.0, reports[2]);
            report(90.0, reports[3]);
        }

        private void report(double percentile, metricsReport report) {
            report.percentile = percentile;
            int count = m_stamps.size();
            if (count == 0) {
                report.sample_count = 0;
                report.minimum = 0;
                report.average = 0;
                report.maximum = 0;
                report.deviation = 0;
            } else {
                int range = (int) ((count * percentile + 50.0) / 100.0);
                double sum = 0.0;
                for (int i = 0; i < range; i++) {
                    sum += m_stamps.get(i);
                }
                double average = sum / range;
                sum = 0.0;
                for (int i = 0; i < range; i++) {
                    double diff = m_stamps.get(i) - average;
                    sum += (diff * diff);
                }
                report.sample_count = range;
                report.minimum = m_stamps.get(0);
                report.average = average;
                assert (range > 0);
                report.maximum = m_stamps.get(range - 1);
                report.deviation = Math.sqrt(sum / (double) range);
            }
        }
    }

    private final Partition m_partition;

    private final QueryConditionMgr m_qos_query;

    private final Dispatcher m_dispatcher = new Dispatcher();

    private final TopicMgr m_topic;

    private final WriterMgr<latency_messageDataWriter> m_writer;

    private final TopicMgr m_echo_topic;

    private final ReaderMgr<latency_messageDataReader> m_reader;

    private final StatusConditionMgr m_reader_condition;

    private final Attachment m_reader_attachment;

    private final Thread m_writer_thread = new Thread() {

        public void run() {
            writer_thread();
        }
    };

    private final Thread m_reader_thread = new Thread() {

        public void run() {
            reader_thread();
        }
    };

    private final Thread m_report_thread = new Thread() {

        public void run() {
            report_thread();
        }
    };

    private boolean m_writer_active = false;

    private boolean m_reader_active = false;

    private boolean m_report_active = false;

    private int m_config_number = 0;

    private transceiverDef m_def;

    private transceiverQos m_qos;

    private final DDS.DataWriterQosHolder m_writer_qos = new DDS.DataWriterQosHolder();

    private final DDS.DataReaderQosHolder m_reader_qos = new DDS.DataReaderQosHolder();

    private double m_previous_time;

    private final Metrics m_send_latency = new Metrics();

    private final Metrics m_echo_latency = new Metrics();

    private final Metrics m_trip_latency = new Metrics();

    private final Metrics m_send_source_latency = new Metrics();

    private final Metrics m_send_arrival_latency = new Metrics();

    private final Metrics m_send_trip_latency = new Metrics();

    private final Metrics m_echo_source_latency = new Metrics();

    private final Metrics m_echo_arrival_latency = new Metrics();

    private final Metrics m_echo_trip_latency = new Metrics();

    private final Metrics m_inter_arrival_time = new Metrics();

    private final latency_messageSeqHolder m_reader_messages = new latency_messageSeqHolder();

    private final SampleInfoSeqHolder m_reader_infos = new SampleInfoSeqHolder();

    public Transceiver(Partition partition) {
        m_partition = partition;
        m_qos_query = new QueryConditionMgr(qos_reader());
        m_topic = new TopicMgr(participant(), new latency_messageTypeSupport());
        m_writer = new WriterMgr<latency_messageDataWriter>(publisher(), m_topic, latency_messageDataWriter.class);
        m_echo_topic = new TopicMgr(participant(), new latency_messageTypeSupport());
        m_reader = new ReaderMgr<latency_messageDataReader>(subscriber(), m_echo_topic, latency_messageDataReader.class);
        m_reader_condition = new StatusConditionMgr(m_reader);
        m_reader_attachment = new Attachment(m_dispatcher, m_reader_condition);
    }

    public void finalize() {
    }

    public void create(transceiverDef def) {
        m_def = def;
        String[] params = new String[1];
        params[0] = Integer.toString(m_def.transceiver_id);
        m_qos_query.create(ANY_SAMPLE_STATE.value, ANY_VIEW_STATE.value, ANY_INSTANCE_STATE.value, "transceiver_id = %0", params);
        set_topics();
        transceiverQosSeqHolder qoss = new transceiverQosSeqHolder();
        SampleInfoSeqHolder infos = new SampleInfoSeqHolder();
        int retcode = qos_reader().value().read_w_condition(qoss, infos, 1, m_qos_query.value());
        if (retcode == RETCODE_NO_DATA.value) {
            m_qos = new transceiverQos();
            m_qos.group_id = m_def.group_id;
            m_qos.transceiver_id = m_def.transceiver_id;
            m_qos.partition_id = m_def.partition_id;
            m_qos.writer_qos.latency_budget.duration.sec = 0;
            m_qos.writer_qos.latency_budget.duration.nanosec = 0;
            m_qos.writer_qos.transport_priority.value = 0;
            m_qos.reader_qos.history.depth = 1;
            m_qos.reader_qos.latency_budget.duration.sec = 0;
            m_qos.reader_qos.latency_budget.duration.nanosec = 0;
            retcode = qos_writer().value().write(m_qos, 0);
            qos_writer().check(retcode, "transceiverQosDataWriter::write");
        } else {
            qos_reader().check(retcode, "transceiverQosDataReader::read_w_condition");
            assert (qoss.value.length == 1);
            assert (infos.value.length == 1);
            m_qos = qoss.value[0];
            assert (m_qos.group_id == m_def.group_id);
            assert (m_qos.transceiver_id == m_def.transceiver_id);
            assert (m_qos.partition_id == m_def.partition_id);
        }
        qos_reader().value().return_loan(qoss, infos);
        set_qos();
        m_writer_active = true;
        m_writer_thread.start();
        m_reader_active = true;
        m_reader_thread.start();
        m_report_active = true;
        m_report_thread.start();
    }

    public void dispose() {
        m_writer_active = false;
        m_reader_active = false;
        m_report_active = false;
        m_dispatcher.shutdown();
        try {
            m_writer_thread.join();
            m_reader_thread.join();
            m_report_thread.join();
        } catch (Exception e) {
            System.err.println("Caught: " + e);
        }
        m_qos_query.destroy();
        m_echo_topic.destroy();
        m_topic.destroy();
    }

    public void update_def(transceiverDef def) {
        assert (m_def.transceiver_id == def.transceiver_id);
        assert (m_writer_active);
        assert (m_reader_active);
        assert (m_report_active);
        if (m_def.scheduling_class != def.scheduling_class || m_def.thread_priority != def.thread_priority || m_def.topic_kind != def.topic_kind || m_def.topic_id != def.topic_id) {
            m_writer_active = false;
            m_reader_active = false;
            m_report_active = false;
            m_dispatcher.shutdown();
            try {
                m_writer_thread.join();
                m_reader_thread.join();
                m_report_thread.join();
            } catch (Exception e) {
                System.err.println("Caught: " + e);
            }
            if (m_def.topic_kind != def.topic_kind || m_def.topic_id != def.topic_id) {
                m_def = def;
                set_topics();
                set_qos();
            } else {
                m_def = def;
            }
            m_config_number++;
            m_writer_active = true;
            m_writer_thread.start();
            m_reader_active = true;
            m_reader_thread.start();
            m_report_active = true;
            m_report_thread.start();
        } else {
            m_def = def;
            m_config_number++;
        }
    }

    public void update_qos(transceiverQos qos) {
        assert (m_def.transceiver_id == qos.transceiver_id);
        assert (m_writer_active);
        assert (m_reader_active);
        assert (m_report_active);
        set_qos();
        if (m_writer.value() != null) {
            int retcode = m_writer.value().set_qos(m_writer_qos.value);
            m_writer.check(retcode, "latency_messageDataWriter::set_qos");
        }
        if (m_reader.value() != null) {
            int retcode = m_reader.value().set_qos(m_reader_qos.value);
            m_reader.check(retcode, "latency_messageDataReader::set_qos");
        }
        m_config_number++;
    }

    public Partition partition() {
        return m_partition;
    }

    public int partition_id() {
        return partition().partition_id();
    }

    public Processor processor() {
        return partition().processor();
    }

    public ParticipantMgr participant() {
        return partition().participant();
    }

    public PublisherMgr publisher() {
        return partition().publisher();
    }

    public SubscriberMgr subscriber() {
        return partition().subscriber();
    }

    public ReaderMgr<transceiverQosDataReader> qos_reader() {
        return processor().transceiver_qos_reader();
    }

    public WriterMgr<transceiverQosDataWriter> qos_writer() {
        return processor().transceiver_qos_writer();
    }

    public WriterMgr<transceiverReportDataWriter> transceiver_report_writer() {
        return processor().transceiver_report_writer();
    }

    private void set_topics() {
        TopicQosHolder topic_qos = new TopicQosHolder();
        int retcode = participant().value().get_default_topic_qos(topic_qos);
        participant().check(retcode, "Participant::get_default_topic_qos");
        String topic_name = null;
        String echo_topic_name = null;
        switch(m_def.topic_kind.value()) {
            case TopicKind._RELIABLE:
                topic_qos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
                topic_name = "LatencyTopic_" + m_def.topic_id + "_R";
                echo_topic_name = "LatencyEchoTopic_" + m_def.topic_id + "_R";
                break;
            case TopicKind._TRANSIENT:
                topic_qos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
                topic_qos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
                topic_name = "LatencyTopic_" + m_def.topic_id + "_T";
                echo_topic_name = "LatencyEchoTopic_" + m_def.topic_id + "_T";
                break;
            case TopicKind._PERSISTENT:
                topic_qos.value.durability.kind = DDS.DurabilityQosPolicyKind.PERSISTENT_DURABILITY_QOS;
                topic_name = "LatencyTopic_" + m_def.topic_id + "_P";
                echo_topic_name = "LatencyEchoTopic_" + m_def.topic_id + "_P";
                break;
            case TopicKind._BEST_EFFORT:
                topic_name = "LatencyTopic_" + m_def.topic_id + "_B";
                echo_topic_name = "LatencyEchoTopic_" + m_def.topic_id + "_B";
                break;
        }
        m_topic.create(topic_name, topic_qos.value);
        m_echo_topic.create(echo_topic_name, topic_qos.value);
    }

    private void set_qos() {
        int retcode = publisher().value().get_default_datawriter_qos(m_writer_qos);
        publisher().check(retcode, "Publisher::get_default_datawriter_qos");
        TopicQosHolder topic_qos = new TopicQosHolder();
        retcode = m_topic.value().get_qos(topic_qos);
        m_topic.check(retcode, "Topic::get_qos");
        retcode = publisher().value().copy_from_topic_qos(m_writer_qos, topic_qos.value);
        publisher().check(retcode, "Publisher::copy_from_topic_qos");
        m_writer_qos.value.latency_budget.duration.sec = m_qos.writer_qos.latency_budget.duration.sec;
        m_writer_qos.value.latency_budget.duration.nanosec = m_qos.writer_qos.latency_budget.duration.nanosec;
        m_writer_qos.value.transport_priority.value = m_qos.writer_qos.transport_priority.value;
        retcode = subscriber().value().get_default_datareader_qos(m_reader_qos);
        subscriber().check(retcode, "Subscriber::get_default_datareader_qos");
        retcode = m_echo_topic.value().get_qos(topic_qos);
        m_echo_topic.check(retcode, "Topic::get_qos");
        retcode = subscriber().value().copy_from_topic_qos(m_reader_qos, topic_qos.value);
        subscriber().check(retcode, "Subscriber::copy_from_topic_qos");
        m_reader_qos.value.history.depth = m_qos.reader_qos.history.depth;
        m_reader_qos.value.latency_budget.duration.sec = m_qos.reader_qos.latency_budget.duration.sec;
        m_reader_qos.value.latency_budget.duration.nanosec = m_qos.reader_qos.latency_budget.duration.nanosec;
    }

    private void writer_thread() {
        try {
            m_writer.create(m_writer_qos.value);
            latency_message message = new latency_message();
            message.application_id = processor().application_id();
            message.random_id = processor().random_id();
            message.transceiver_id = m_def.transceiver_id;
            message.sequence_number = 0;
            int last_size = 0;
            while (m_writer_active) {
                if (m_def.message_size != last_size) {
                    int size = 76;
                    size = (m_def.message_size > size) ? m_def.message_size - size : 1;
                    last_size = m_def.message_size;
                    message.payload_data = new char[size];
                    for (int i = 0; i < size; i++) {
                        message.payload_data[i] = (char) (65 + (((i / 2) % 26) + (i % 2) * 32));
                    }
                }
                message.sequence_number++;
                message.config_number = m_config_number;
                message.write_timestamp = processor().get_timestamp();
                int retcode = m_writer.value().write(message, 0);
                m_writer.check(retcode, "latency_messageDataWriter::write");
                Thread.sleep(m_def.write_period);
            }
        } catch (DDSError error) {
            processor().report_error(error, partition_id(), m_def.transceiver_id);
            System.err.println("Transceiver writer thread exiting: " + error);
            error.printStackTrace();
        } catch (Exception e) {
            System.err.println("Transceiver writer thread exiting: " + e);
            e.printStackTrace();
        } finally {
            m_writer.destroy();
        }
    }

    private void reader_thread() {
        try {
            m_reader.create(m_reader_qos.value);
            m_reader_condition.get();
            m_reader_condition.value().set_enabled_statuses(DATA_AVAILABLE_STATUS.value);
            m_reader_attachment.attach(new Handler() {

                public boolean handle_condition(Condition condition) {
                    return read_latency_message(condition);
                }
            });
            m_previous_time = 0;
            m_dispatcher.run();
        } catch (DDSError error) {
            processor().report_error(error, partition_id(), m_def.transceiver_id);
            System.err.println("Transceiver reader thread exiting: " + error);
            error.printStackTrace();
        } catch (Exception e) {
            System.err.println("Transceiver reader thread exiting: " + e);
            e.printStackTrace();
        } finally {
            m_reader_attachment.detach();
            m_reader_condition.release();
            m_reader.destroy();
        }
    }

    public boolean read_latency_message(Condition condition) {
        int retcode = m_reader.value().take(m_reader_messages, m_reader_infos, 1, ANY_SAMPLE_STATE.value, ANY_VIEW_STATE.value, ANY_INSTANCE_STATE.value);
        m_reader.check(retcode, "latency_messageDataReader::take");
        double read_time = processor().get_timestamp();
        int length = m_reader_messages.value.length;
        assert (length == m_reader_infos.value.length);
        for (int i = 0; i < length; i++) {
            latency_message message = m_reader_messages.value[i];
            SampleInfo info = m_reader_infos.value[i];
            double write_time = message.write_timestamp;
            double echo_time = message.echo_timestamp;
            double source_time = Processor.to_timestamp(info.source_timestamp);
            double arrival_time = read_time;
            m_send_latency.add(message.send_latency);
            m_echo_latency.add(read_time - echo_time);
            m_trip_latency.add(read_time - write_time);
            m_send_source_latency.add(message.source_latency);
            m_send_arrival_latency.add(message.arrival_latency);
            m_send_trip_latency.add(message.send_latency - message.source_latency - message.arrival_latency);
            m_echo_source_latency.add(source_time - echo_time);
            m_echo_arrival_latency.add(read_time - arrival_time);
            m_echo_trip_latency.add(arrival_time - source_time);
            if (m_previous_time != 0) {
                m_inter_arrival_time.add(read_time - m_previous_time);
            }
            m_previous_time = read_time;
        }
        m_reader.value().return_loan(m_reader_messages, m_reader_infos);
        return true;
    }

    private void report_thread() {
        try {
            transceiverReport report = new transceiverReport();
            report.application_id = processor().application_id();
            report.transceiver_id = m_def.transceiver_id;
            report.partition_id = partition_id();
            SampleLostStatusHolder sl_status = new SampleLostStatusHolder();
            SampleRejectedStatusHolder sr_status = new SampleRejectedStatusHolder();
            RequestedDeadlineMissedStatusHolder rdm_status = new RequestedDeadlineMissedStatusHolder();
            OfferedDeadlineMissedStatusHolder odm_status = new OfferedDeadlineMissedStatusHolder();
            while (m_report_active) {
                report.reader_status.samples_lost = 0;
                report.reader_status.samples_rejected = 0;
                report.reader_status.deadlines_missed = 0;
                report.writer_status.deadlines_missed = 0;
                m_send_latency.init();
                m_echo_latency.init();
                m_trip_latency.init();
                m_send_source_latency.init();
                m_send_arrival_latency.init();
                m_send_trip_latency.init();
                m_echo_source_latency.init();
                m_echo_arrival_latency.init();
                m_echo_trip_latency.init();
                m_inter_arrival_time.init();
                Thread.sleep(m_def.report_period);
                m_send_latency.analyze(report.send_latency);
                m_echo_latency.analyze(report.echo_latency);
                m_trip_latency.analyze(report.trip_latency);
                m_send_source_latency.analyze(report.send_source_latency);
                m_send_arrival_latency.analyze(report.send_arrival_latency);
                m_send_trip_latency.analyze(report.send_trip_latency);
                m_echo_source_latency.analyze(report.echo_source_latency);
                m_echo_arrival_latency.analyze(report.echo_arrival_latency);
                m_echo_trip_latency.analyze(report.echo_trip_latency);
                m_inter_arrival_time.analyze(report.inter_arrival_time);
                if (m_reader.value() != null) {
                    int retcode = m_reader.value().get_sample_lost_status(sl_status);
                    m_reader.check(retcode, "latency_messageDataReader::get_sample_lost_status");
                    report.reader_status.samples_lost = sl_status.value.total_count_change;
                    retcode = m_reader.value().get_sample_rejected_status(sr_status);
                    m_reader.check(retcode, "latency_messageDataReader::get_sample_rejected_status");
                    report.reader_status.samples_rejected = sr_status.value.total_count_change;
                    retcode = m_reader.value().get_requested_deadline_missed_status(rdm_status);
                    m_reader.check(retcode, "latency_messageDataReader::get_requested_deadline_missed_status");
                    report.reader_status.deadlines_missed = rdm_status.value.total_count_change;
                }
                if (m_writer.value() != null) {
                    int retcode = m_writer.value().get_offered_deadline_missed_status(odm_status);
                    m_writer.check(retcode, "latency_messageDataWriter::get_offered_deadline_missed_status");
                    report.writer_status.deadlines_missed = odm_status.value.total_count_change;
                }
                report.config_number = m_config_number;
                int retcode = transceiver_report_writer().value().write(report, 0);
                transceiver_report_writer().check(retcode, "TransceiverReportDataWriter::write");
            }
        } catch (DDSError error) {
            processor().report_error(error, partition_id(), m_def.transceiver_id);
            System.err.println("Transceiver report thread exiting: " + error);
            error.printStackTrace();
        } catch (Exception e) {
            System.err.println("Transceiver report thread exiting: " + e);
            e.printStackTrace();
        }
    }
}
