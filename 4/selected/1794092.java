package iwallet.client.gui;

import iwallet.client.gui.NavigationView.TreeObject;
import iwallet.client.gui.NavigationView.TreeParent;
import iwallet.common.account.AccountBill;
import iwallet.common.account.AccountEntry;
import iwallet.common.account.IncomeAccountEntry;
import iwallet.common.account.OutcomeAccountEntry;
import iwallet.common.currency.CurrencyType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * @author �ų�
 *
 */
public class SearchActionGroup extends ActionGroup {

    private TableViewer tv;

    private Action detailAction = new DetailAction();

    private Action[] curTypeAction;

    private Action incomeAction = new IncomeAction();

    private Action payoutAction = new PayoutAction();

    private Action clearAction = new NoFilterAction();

    private Action exportAction = new ExportAction();

    private ViewerFilter incomeFilter = new IncomeFilter();

    private ViewerFilter payoutFilter = new PayoutFilter();

    private CurrencyTypeFilter[] curTypeFilter;

    public SearchActionGroup(TableViewer tv) {
        this.tv = tv;
        CurrencyType[] type = CurrencyType.values();
        curTypeFilter = new CurrencyTypeFilter[type.length];
        curTypeAction = new CurrencyTypeAction[type.length];
        for (int i = 0; i != type.length; ++i) {
            curTypeFilter[i] = new CurrencyTypeFilter(type[i]);
            curTypeAction[i] = new CurrencyTypeAction(curTypeFilter[i]);
        }
    }

    public void doubleClick() {
        detailAction.run();
    }

    public void fillContextMenu(IMenuManager mgr) {
        MenuManager manager = (MenuManager) mgr;
        manager.add(detailAction);
        manager.add(exportAction);
        manager.add(new Separator());
        MenuManager subManager = new MenuManager("���׹���");
        subManager.add(incomeAction);
        subManager.add(payoutAction);
        for (Action action : curTypeAction) {
            subManager.add(action);
        }
        subManager.add(clearAction);
        manager.add(subManager);
        Table table = tv.getTable();
        Menu menu = manager.createContextMenu(table);
        table.setMenu(menu);
    }

    public void fillActionToolBars(ToolBarManager toolBarManager) {
        toolBarManager.add(createActionContrItem(detailAction));
        toolBarManager.add(createActionContrItem(exportAction));
        toolBarManager.update(true);
    }

    private ActionContributionItem createActionContrItem(IAction action) {
        ActionContributionItem aci = new ActionContributionItem(action);
        return aci;
    }

    /**
     * @author �ų�
     *
     */
    private class NoFilterAction extends Action {

        public NoFilterAction() {
            setText("��ʾ���н���");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(ImageContext.LISTALL));
        }

        public void run() {
            tv.removeFilter(incomeFilter);
            tv.removeFilter(payoutFilter);
            for (CurrencyTypeFilter filter : curTypeFilter) {
                tv.removeFilter(filter);
            }
        }
    }

    /**
     * @author �ų�
     *
     */
    private class IncomeAction extends Action {

        public IncomeAction() {
            setText("����ʾ����");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(ImageContext.INCOME));
        }

        public void run() {
            tv.addFilter(incomeFilter);
        }
    }

    /**
     * @author �ų�
     *
     */
    private class PayoutAction extends Action {

        public PayoutAction() {
            setText("����ʾ֧��");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(ImageContext.PAYOUT));
        }

        public void run() {
            tv.addFilter(payoutFilter);
        }
    }

    /**
     * @author �ų�
     *
     */
    private class CurrencyTypeAction extends Action {

        private CurrencyTypeFilter curTypeFilter;

        public CurrencyTypeAction(CurrencyTypeFilter filter) {
            setText("���г�" + filter.getCurrencyType().getFullName() + "����");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(filter.getCurrencyType().toString()));
            curTypeFilter = filter;
        }

        public void run() {
            tv.addFilter(curTypeFilter);
        }
    }

    /**
     * @author �ų�
     *
     */
    private class DetailAction extends Action {

        public DetailAction() {
            setText("������ϸ");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(ImageContext.MODIFYENTRY));
        }

        public void run() {
            IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
            AccountEntry entry = (AccountEntry) sel.getFirstElement();
            if (entry == null) {
                return;
            }
            TitleAreaDialog dialog = new DetailEntryDialog(null, entry);
            dialog.open();
        }
    }

    /**
     * �������ű��Excel�ļ�
     * @author ����
     */
    private class ExportAction extends Action {

        public ExportAction() {
            super();
            setText("������Excel");
            setHoverImageDescriptor(ImageContext.getImageDescriptor(ImageContext.EXPORT));
        }

        @Override
        public void run() {
            List<AccountEntry> entries = (List<AccountEntry>) tv.getInput();
            NavigationView nav = (NavigationView) Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(NavigationView.ID);
            IStructuredSelection sel = (IStructuredSelection) nav.getTreeViewer().getSelection();
            TreeObject node = (TreeObject) sel.getFirstElement();
            if (node == null || node instanceof TreeParent) {
                return;
            }
            AccountBill bill = node.getAccountBill();
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet(bill.getBillName());
            int rownum = 0;
            HSSFRow row = sheet.createRow(rownum++);
            row.createCell((short) 0).setCellValue(new HSSFRichTextString("���"));
            row.createCell((short) 1).setCellValue(new HSSFRichTextString("���"));
            row.createCell((short) 2).setCellValue(new HSSFRichTextString("����"));
            row.createCell((short) 3).setCellValue(new HSSFRichTextString("�����"));
            row.createCell((short) 4).setCellValue(new HSSFRichTextString("��������"));
            row.createCell((short) 5).setCellValue(new HSSFRichTextString("��¼����"));
            row.createCell((short) 6).setCellValue(new HSSFRichTextString("��ע"));
            for (AccountEntry entry : entries) {
                row = sheet.createRow(rownum++);
                if (entry instanceof IncomeAccountEntry) {
                    row.createCell((short) 0).setCellValue(new HSSFRichTextString("����"));
                } else {
                    row.createCell((short) 0).setCellValue(new HSSFRichTextString("֧��"));
                }
                row.createCell((short) 1).setCellValue(entry.getEntryID());
                row.createCell((short) 2).setCellValue(new HSSFRichTextString(entry.getMoney().getType().toString()));
                row.createCell((short) 3).setCellValue(entry.getMoney().getValue());
                HSSFCell cell = row.createCell((short) 4);
                cell.setCellValue(entry.getDate());
                HSSFCellStyle style = wb.createCellStyle();
                style.setDataFormat(wb.createDataFormat().getFormat("yyyy-m-d"));
                cell.setCellStyle(style);
                cell = row.createCell((short) 5);
                cell.setCellValue(entry.getCreateDate());
                cell.setCellStyle(style);
                row.createCell((short) 6).setCellValue(new HSSFRichTextString(entry.getDescription()));
            }
            FileOutputStream fileOut;
            final Shell shell = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
            try {
                FileDialog dialog = new FileDialog(shell, SWT.SAVE);
                dialog.setFileName("iwallet.xls");
                dialog.setFilterNames(new String[] { "Microsoft Excel (*.xls)" });
                dialog.setFilterExtensions(new String[] { "*.xls" });
                String fileName = dialog.open();
                if (fileName == null) {
                    return;
                }
                if (new File(fileName).exists()) {
                    if (!MessageDialog.openQuestion(shell, "iWallet - Question", "This file already exists. Do you want to overwrite?")) {
                        return;
                    }
                }
                fileOut = new FileOutputStream(fileName);
                wb.write(fileOut);
                fileOut.close();
                MessageDialog.openInformation(shell, "iWallet - Information", "Exportation finished successfully. :)");
            } catch (FileNotFoundException e) {
                MessageDialog.openInformation(shell, "Oops...", "File not found! :(");
            } catch (IOException e) {
                MessageDialog.openInformation(shell, "Oops...", "An I/O exception has been caught. :(");
            }
            super.run();
        }
    }

    /**
     * @author �ų�
     *
     */
    private class IncomeFilter extends ViewerFilter {

        public boolean select(Viewer viewer, Object parent, Object element) {
            AccountEntry entry = (AccountEntry) element;
            if (entry instanceof IncomeAccountEntry) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @author �ų�
     *
     */
    private class PayoutFilter extends ViewerFilter {

        public boolean select(Viewer viewer, Object parent, Object element) {
            AccountEntry entry = (AccountEntry) element;
            if (entry instanceof OutcomeAccountEntry) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @author �ų�
     *
     */
    private class CurrencyTypeFilter extends ViewerFilter {

        CurrencyType type;

        public CurrencyTypeFilter(CurrencyType type) {
            this.type = type;
        }

        public boolean select(Viewer viewer, Object parent, Object element) {
            AccountEntry entry = (AccountEntry) element;
            if (entry.getMoney().getType().equals(type)) {
                return true;
            } else {
                return false;
            }
        }

        public CurrencyType getCurrencyType() {
            return type;
        }
    }

    /**
     * @author �ų�
     *
     */
    public class DetailEntryDialog extends TitleAreaDialog {

        private AccountEntry entry;

        private Label createTime, id, type, modifyTime, time;

        private Label user, bill, book, blc;

        private Label amount, desc;

        private Label currencyType;

        public DetailEntryDialog(Shell parentShell, AccountEntry entry) {
            super(parentShell);
            this.entry = entry;
        }

        protected Control createDialogArea(Composite parent) {
            getShell().setText("��������");
            setTitle("��������");
            setMessage("�鿴��������", IMessageProvider.INFORMATION);
            Composite topComp = new Composite(parent, SWT.NONE);
            topComp.setLayoutData(new GridData(GridData.CENTER, GridData.END, true, true));
            topComp.setLayout(new GridLayout(2, true));
            Composite comp1 = new Composite(topComp, SWT.NONE);
            comp1.setLayoutData(new GridData(GridData.FILL_BOTH));
            comp1.setLayout(new GridLayout(2, false));
            new Label(comp1, SWT.NONE).setText("�û�: ");
            user = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("�˲�ID: ");
            book = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("�ʻ�ID: ");
            bill = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("����ID: ");
            id = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("��������: ");
            type = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("������֧: ");
            blc = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("��������: ");
            currencyType = new Label(comp1, SWT.READ_ONLY);
            new Label(comp1, SWT.NONE).setText("���׽��: ");
            amount = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("��������: ");
            time = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("���׽���ʱ��: ");
            createTime = new Label(comp1, SWT.NONE);
            new Label(comp1, SWT.NONE).setText("����޸�ʱ��: ");
            modifyTime = new Label(comp1, SWT.NONE);
            Group group = new Group(topComp, SWT.NONE);
            group.setText("Description");
            group.setLayoutData(new GridData(GridData.FILL_BOTH));
            group.setLayout(new GridLayout(2, false));
            desc = new Label(group, SWT.BORDER);
            desc.setLayoutData(new GridData(GridData.FILL_BOTH));
            setValue();
            return topComp;
        }

        private void setValue() {
            if (entry == null) {
                return;
            }
            user.setText(entry.getUserName());
            book.setText(entry.getBookID() + "");
            bill.setText(entry.getBillID() + "");
            id.setText(String.valueOf(entry.getEntryID()));
            type.setText(entry.getTypeName());
            if (entry instanceof IncomeAccountEntry) {
                blc.setText("����");
                blc.setImage(ImageContext.getImage(ImageContext.INCOME));
            } else {
                blc.setText("֧��");
                blc.setImage(ImageContext.getImage(ImageContext.PAYOUT));
            }
            currencyType.setText(entry.getMoney().getType().getFullName());
            amount.setText(String.valueOf(entry.getMoney().getValue()));
            time.setText(GuiParameter.translateDateFormat(entry.getDate()));
            createTime.setText(GuiParameter.translateDateFormat(entry.getCreateDate()));
            modifyTime.setText(GuiParameter.translateDateFormat(entry.getModifyDate()));
            desc.setText(entry.getDescription());
        }

        protected void buttonPressed(int btnId) {
            super.buttonPressed(btnId);
        }
    }
}
