package com.cameocontrol.cameo.action;

import com.cameocontrol.cameo.control.CameoTime;
import com.cameocontrol.cameo.control.ChannelSet;
import com.cameocontrol.cameo.control.Console;
import com.cameocontrol.cameo.output.OutputManager;
import com.cameocontrol.cameo.resource.ResourceManager;

public class ExeVisitor implements IVisitor {

    private Console _console;

    private OutputManager _output;

    private ResourceManager _resources;

    public ExeVisitor(Console c, OutputManager om, ResourceManager rm) {
        _console = c;
        _output = om;
        _resources = rm;
    }

    public void doIt(ACTAction n) {
        n.visit(this);
    }

    private short int2short(int i) {
        return (short) (i * 2.556);
    }

    public void applyACTChanSelect(ACTChanSelect n) {
        _console.select(n._channels.getChannels());
    }

    public void applyACTChanAtLevel(ACTAtLevel n) {
        if (!n.hasSelection()) _console.at(int2short(n._level)); else _console.at(n._channels.getChannels(), int2short(n._level));
    }

    public void applyACTChanAtCue(ACTAtCue n) {
        if (!n.hasSelection()) _console.at(int2short(n._level)); else _console.at(n._channels.getChannels(), n._level);
    }

    public void applyACTOut(ACTOut n) {
        if (!n.hasSelection()) _console.out(); else _console.out(n._channels.getChannels());
    }

    public void applyACTPlus(ACTPlus n) {
        if (!n.hasSelection()) _console.plus(int2short(n._level)); else _console.plus(n._channels.getChannels(), int2short(n._level));
    }

    public void applyACTMinus(ACTMinus n) {
        if (!n.hasSelection()) _console.minus(int2short(n._level)); else _console.minus(n._channels.getChannels(), int2short(n._level));
    }

    public void applyACTBack(ACTBack n) {
        _console.back();
    }

    public void applyACTDeleteCue(ACTDeleteCue n) {
        _console.deleteCue(n._cue._number);
    }

    public void applyACTGo(ACTGo n) {
        _console.go();
    }

    public void applyACTGotoCue(ACTGotoCue n) {
        if (n._cue._upTime < 0 || n._cue._downTime < 0) _console.gotoCue(n._cue._number); else _console.gotoCue(n._cue._number, new CameoTime(n._cue._upTime, n._cue._downTime, 0, 0));
    }

    public void applyACTLoadCue(ACTLoadCue n) {
        _console.loadCue(n._cue._number);
    }

    public void applyACTRecordCue(ACTRecordCue n) {
        CameoTime t = null;
        if (n._cue._number < 1) {
            _console.recordCue();
        } else {
            if (n._cue._upTime > 0 || n._cue._downTime > 0) {
                if (n._cue._upTimeDelay > 0 || n._cue._downTimeDelay > 0) t = new CameoTime(n._cue._upTime, n._cue._downTime, n._cue._upTimeDelay, n._cue._downTimeDelay); else t = new CameoTime(n._cue._upTime, n._cue._downTime, 0, 0);
            }
            if (t == null) _console.recordCue(n._cue._number, n._cue._nextCue); else _console.recordCue(n._cue._number, t, n._cue._nextCue);
            if (n._cue._followTime >= 0) _console.follow(n._cue._number, n._cue._followTime);
        }
    }

    public void applyACTCueMod(ACTCueMod n) {
        if (!n.cueNumber()) {
            if (n.timeUpdate()) _console.time(n._upTime, n._downTime);
            if (n.delayUpdate()) _console.delay(n._upTimeDelay, n._downTimeDelay);
            if (n.followUpdate()) _console.follow(n._followTime);
            if (n.linkUpdate()) _console.link(n._nextCue);
            if (n.descriptionUpdate()) _console.description(n._description);
        } else {
            if (n.timeUpdate()) _console.time(n._number, n._upTime, n._downTime);
            if (n.delayUpdate()) _console.delay(n._number, n._upTimeDelay, n._downTimeDelay);
            if (n.followUpdate()) _console.follow(n._number, n._followTime);
            if (n.linkUpdate()) _console.link(n._number, n._nextCue);
            if (n.descriptionUpdate()) _console.description(n._number, n._description);
        }
    }

    public void applyACTCueMove(ACTCueMove n) {
        _console.copyCue(n._cueNum, n._newCueNum);
        _console.deleteCue(n._cueNum);
    }

    public void applyACTDimAtLevel(ACTDimAtLevel n) {
        _output.dimAt(n._dimmer, int2short(n._level));
    }

    public void applyACTPatchChan(ACTPatchChan n) {
        _output.patchChan(n._channel, n._dimmer);
    }

    public void applyACTUnpatchDim(ACTUnpatchDim n) {
        _output.unpatchDim(n._dimmer);
    }

    public void applyACTUnpatchChan(ACTUnpatchChan n) {
        _output.unpatchChan(n._channel);
    }

    public void applyACTPatch1to1(ACTPatch1to1 n) {
        _output.patch1to1();
    }

    public void applyACTNext(ACTNext n) {
        _console.out(new ChannelSet(n._currentChan));
        _console.at(new ChannelSet(n._nextChan), n._level);
    }

    public void applyACTPrevious(ACTPrevious n) {
        _console.out(new ChannelSet(n._currentChan));
        _console.at(new ChannelSet(n._nextChan), n._level);
    }

    public void applyACTSettingsShowGenralMod(ACTSettingsShowGenralMod n) {
        if (n.recordModeUpdate()) _console.recordMode(n._recordMode);
        if (n.channelUpdate()) _console.totalChannels(n._channels);
        if (n.dimmerUpdate()) _console.totalDimmers(n._dimmers);
        if (n.upTimeUpdate()) _console.defaultUpTime(n._upTime);
        if (n.downTimeUpdate()) _console.defaultDownTime(n._downTime);
        if (n.gotoCueTimeUpdate()) _console.defaultGotoCueTime(n._gotoCueTime);
        if (n.titleUpdate()) _console.showTitle(n._title);
        if (n.commentUpdate()) _console.showComment(n._comment);
    }

    public void applyACTSettingsShowChannelMod(ACTSettingsShowChannelMod n) {
        if (n.perLineUpdate()) _console.channelsPerLine(n._chanPerLine);
        if (n.perHGroupUpdate()) _console.channelsHGroup(n._chanPerHGroup);
        if (n.perVGroupUpdate()) _console.channelsVGroup(n._chanPerVGroup);
    }

    public void applyACTSettingsPrefMod(ACTSettingsPrefMod n) {
        if (n.middleMouseActionUpdate()) _console.middleMouseAction(n._middleMouseAction);
        if (n.refreshRateUpdate()) _output.refreshRate(n._refreshRate);
        if (n.startCodeUpdate()) _output.startCode((short) n._startCode);
        _resources.savePrefrences();
    }

    public void applyACTPatchClear(ACTPatchClear n) {
        _output.patchClear();
    }

    public void applyACTCuesClear(ACTCuesClear n) {
        _console.deleteAllCues();
    }

    public void applyACTChannelSelection(ACTChanSelection n) {
    }

    public void applyACTCue(ACTCue n) {
    }

    public void applyACTLevelMod(ACTLevelMod n) {
    }

    public void applyACTChannel(ACTChannel n) {
    }

    public void applyACTChannelBinOp(ACTChanBinOp n) {
    }

    public void applyACTChanThru(ACTChanThru n) {
    }

    public void applyACTChanExcept(ACTChanExcept n) {
    }

    public void applyACTChanAnd(ACTChanAnd n) {
    }

    public void applyACTNoOp(ACTNoOp n) {
    }

    public void applyACTSaveShow(ACTSaveShow n) {
        if (n.isSaveAs()) _resources.saveAs(n._fileName); else _resources.save();
    }

    public void applyACTLoadShow(ACTLoadShow n) {
        _resources.load(n._fileName);
    }

    public void applyACTChanLocation(ACTChanLocation n) {
        _console.channelLocation(n._channel, n._x, n._y);
    }
}
