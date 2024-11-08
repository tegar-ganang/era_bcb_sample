package cossak;

import javax.swing.tree.*;
import javax.swing.*;
import java.lang.*;
import java.awt.Component;
import org.omg.CosNaming.*;

/**

 *

 * @author hefferjr

 */
public class NameServiceBrowserAdv {

    DefaultTreeModel _mdl;

    NameServiceTreeNode _rootnode;

    org.omg.CORBA.ORB _orb;

    public java.util.HashMap _hashmap = new java.util.HashMap();

    public rthreadpool _tp = new rthreadpool();

    /** Creates a new instance of NameServiceBrowser */
    public NameServiceBrowserAdv(org.omg.CORBA.ORB orb, DefaultTreeModel mdl, NameServiceTreeNode the_root, int threadcount) {
        _tp.add_threads(threadcount);
        _mdl = mdl;
        _orb = orb;
        _rootnode = the_root;
        _mdl.setRoot(_rootnode);
    }

    public void clear(NameServiceTreeNode startnode) {
        if (startnode == null) {
            startnode = _rootnode;
        }
        for (int i = 0; i < startnode.getChildCount(); i++) {
            NameServiceTreeNode tmp = (NameServiceTreeNode) _mdl.getChild(startnode, i);
            System.out.println("clearing " + startnode._quickname + " flav=" + startnode._flavor);
            if (tmp._flavor < 4) {
                System.out.println("   recursing into child " + tmp._quickname + " flav=" + tmp._flavor);
                clear(tmp);
            } else {
                System.out.println("   removing child: " + tmp._quickname + " flav=" + tmp._flavor);
                _mdl.removeNodeFromParent(tmp);
            }
        }
        _hashmap = new java.util.HashMap();
    }

    private class namingcontext_tpwork implements java.lang.Runnable {

        public NameServiceTreeNode _currnode;

        public NamingContext _nc;

        public NameServiceBrowserAdv _parent;

        public org.omg.CORBA.Object _obj;

        namingcontext_tpwork(NameServiceTreeNode currnode, org.omg.CORBA.Object obj, NameServiceBrowserAdv parent) {
            _obj = obj;
            _currnode = currnode;
            _parent = parent;
        }

        public void run() {
            try {
                StatusLogFrm.defaultStatusLogFrm.addLine("trying to narrow NamingContext (1): " + _currnode._quickname + " thread_id=" + java.lang.Thread.currentThread().getId());
                _nc = NamingContextExtHelper.narrow(_obj);
                StatusLogFrm.defaultStatusLogFrm.addLine("successfully narrowed NamingContext" + " thread_id=" + java.lang.Thread.currentThread().getId());
                _parent.add_naming_context_parallel(_currnode, _nc);
            } catch (Exception ex) {
                StatusLogFrm.defaultStatusLogFrm.addLine("error narrowing NamingContext" + " thread_id=" + java.lang.Thread.currentThread().getId());
                _nc = null;
                _currnode.setAllowsChildren(false);
            }
        }
    }

    private class object_tpwork implements java.lang.Runnable {

        public NameServiceTreeNode _currnode;

        public NamingContext _nc;

        public NameServiceBrowserAdv _parent;

        public org.omg.CORBA.Object _obj;

        public Binding _binding;

        object_tpwork(NameServiceTreeNode currnode, NamingContext nc, NameServiceBrowserAdv parent, Binding bind) {
            _nc = nc;
            _currnode = currnode;
            _parent = parent;
            _binding = bind;
        }

        public void run() {
            try {
                if (_currnode._needs_work > 0) {
                    StatusLogFrm.defaultStatusLogFrm.addLine("waiting for parent to be process in object_tpwork");
                    java.lang.Thread.sleep(100);
                    StatusLogFrm.defaultStatusLogFrm.addLine("re-adding pending workpiece ");
                    _parent._tp.add_work(this);
                    return;
                }
                StatusLogFrm.defaultStatusLogFrm.addLine("parent node ready to be processed in object_tpwork");
            } catch (Exception ex) {
                StatusLogFrm.defaultStatusLogFrm.addLine("error narrowing NamingContext");
                _nc = null;
            }
            if (_nc != null) {
                StatusLogFrm.defaultStatusLogFrm.addLine("calling 'add_object_parallel()' on thread: " + java.lang.Thread.currentThread().getId());
                _parent.add_object_parallel(_currnode, _nc, _binding);
            } else {
                StatusLogFrm.defaultStatusLogFrm.addLine("NamingContext is null, skipping 'add_object_parallel()'.");
            }
        }
    }

    public void sweep(NameServiceTreeNode startnode) {
        sweep(startnode, 1);
    }

    public void sweep(NameServiceTreeNode startnode, int depth) {
        if (startnode == null) {
            startnode = _rootnode;
        }
        if (depth == 1) {
            clear(startnode);
        }
        NamingContext nc;
        String currnameservicestring;
        for (int i = 0; i < startnode.getChildCount(); i++) {
            NameServiceTreeNode tmp = (NameServiceTreeNode) _mdl.getChild(startnode, i);
            if (tmp._flavor == 1) {
                sweep(tmp, depth + 1);
                continue;
            }
            currnameservicestring = tmp._ior;
            try {
                org.omg.CORBA.Object o = _orb.string_to_object(tmp._ior);
                NameServiceTreeNode tnode = tmp;
                if (tmp._flavor == 2) {
                    StatusLogFrm.defaultStatusLogFrm.addLine("adding NS workpiece");
                    namingcontext_tpwork w = new namingcontext_tpwork(tnode, o, this);
                    _tp.add_work(w);
                }
            } catch (Exception ex) {
                StatusLogFrm.defaultStatusLogFrm.addLine("Error contacting NamingService: " + currnameservicestring);
                ex.printStackTrace();
            }
        }
        if (startnode == _rootnode) {
            StatusLogFrm.defaultStatusLogFrm.addLine("Main thread returning.");
        }
    }

    public void add_object_parallel(NameServiceTreeNode currnode, NamingContext nc, Binding binding) {
        try {
            org.omg.CORBA.Object obj = nc.resolve(binding.binding_name);
            String objStr = _orb.object_to_string(obj);
            int lastIx = binding.binding_name.length - 1;
            String pretty_name = binding.binding_name[lastIx].id + "." + binding.binding_name[lastIx].kind;
            System.out.println("Found object: " + pretty_name);
            name_node newnode = new name_node(pretty_name, objStr);
            String owner_nc_ior = _orb.object_to_string(nc);
            NameServiceTreeNode tnode = new NameServiceTreeNode(newnode);
            tnode._naming_context_ior = owner_nc_ior;
            if (binding.binding_type == BindingType.ncontext) {
                tnode._flavor = 3;
            } else {
                tnode._flavor = 4;
            }
            tnode._ior = objStr;
            _hashmap.put(tnode._ior, tnode);
            tnode._quickname = pretty_name;
            org.omg.CORBA.Object obj2 = _orb.string_to_object(newnode._objstr);
            String final_type = this.get_type_string(obj2);
            try {
                IDLTypePluginMap.DiscoverDeepInterfaces(obj2, final_type);
            } catch (Exception ex2) {
                System.out.println("exception in NameServiceBrowserAdv, calling DiscoverDeepInterfaces");
                StatusLogFrm.defaultStatusLogFrm.addLine("error calling DiscoverDeepInterfaces (unreachable object?)");
            }
            _mdl.insertNodeInto(tnode, currnode, 0);
            if (binding.binding_type == BindingType.ncontext) {
                tnode._flavor = 3;
                newnode._ping_status = 0;
                namingcontext_tpwork w = new namingcontext_tpwork(tnode, obj, this);
                _tp.add_work(w);
            } else {
                tnode._binding_name = binding.binding_name;
                newnode._ping_status = 1;
                cossak.util.IorPrinter iorp = new cossak.util.IorPrinter(objStr);
                String proftype = iorp.getTypeId();
                System.out.println("Examining " + proftype);
                if (proftype.contains("EventChannelFactory:")) {
                    newnode._is_notif = true;
                    newnode._notif_type = MainFrame.CHANNEL_FACTORY_TYPE;
                }
                if (proftype.contains("EventChannel:")) {
                    newnode._is_notif = true;
                    newnode._notif_type = MainFrame.CHANNEL_TYPE;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void add_naming_context_parallel(NameServiceTreeNode currnode, NamingContext nc) {
        StatusLogFrm.defaultStatusLogFrm.addLine("calling add_naming_context_parallel");
        BindingListHolder bl = new BindingListHolder();
        BindingIteratorHolder bi = new BindingIteratorHolder();
        nc.list(1000, bl, bi);
        Binding bindings[] = bl.value;
        if (bindings.length == 0) {
            StatusLogFrm.defaultStatusLogFrm.addLine("empty naming_context");
            return;
        }
        for (int j = 0; j < bindings.length; j++) {
            Binding b = bindings[j];
            object_tpwork w = new object_tpwork(currnode, nc, this, b);
            StatusLogFrm.defaultStatusLogFrm.addLine("adding object_tpwork");
            _tp.add_work(w);
        }
        currnode._needs_work--;
    }

    public void add_naming_context(NameServiceTreeNode currnode, NamingContext nc) {
        BindingListHolder bl = new BindingListHolder();
        BindingIteratorHolder bi = new BindingIteratorHolder();
        nc.list(1000, bl, bi);
        Binding bindings[] = bl.value;
        if (bindings.length == 0) {
            return;
        }
        for (int j = 0; j < bindings.length; j++) {
            try {
                org.omg.CORBA.Object obj = nc.resolve(bindings[j].binding_name);
                String objStr = _orb.object_to_string(obj);
                int lastIx = bindings[j].binding_name.length - 1;
                String pretty_name = bindings[j].binding_name[lastIx].id + "." + bindings[j].binding_name[lastIx].kind;
                System.out.println("Found object: " + pretty_name);
                name_node newnode = new name_node(pretty_name, objStr);
                String owner_nc_ior = _orb.object_to_string(nc);
                NameServiceTreeNode tnode = new NameServiceTreeNode(newnode);
                tnode._naming_context_ior = owner_nc_ior;
                tnode._flavor = 3;
                tnode._ior = objStr;
                _hashmap.put(tnode._ior, tnode);
                tnode._quickname = pretty_name;
                org.omg.CORBA.Object obj2 = _orb.string_to_object(newnode._objstr);
                String final_type = this.get_type_string(obj2);
                IDLTypePluginMap.DiscoverDeepInterfaces(obj2, final_type);
                _mdl.insertNodeInto(tnode, currnode, 0);
                if (bindings[j].binding_type == BindingType.ncontext) {
                    newnode._ping_status = 0;
                    namingcontext_tpwork w = new namingcontext_tpwork(tnode, obj, this);
                    _tp.add_work(w);
                } else {
                    tnode._binding_name = bindings[j].binding_name;
                    newnode._ping_status = 1;
                    cossak.util.IorPrinter iorp = new cossak.util.IorPrinter(objStr);
                    String proftype = iorp.getTypeId();
                    System.out.println("Examining " + proftype);
                    if (proftype.contains("EventChannelFactory:")) {
                        newnode._is_notif = true;
                        newnode._notif_type = MainFrame.CHANNEL_FACTORY_TYPE;
                    }
                    if (proftype.contains("EventChannel:")) {
                        newnode._is_notif = true;
                        newnode._notif_type = MainFrame.CHANNEL_TYPE;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public String get_type_string(org.omg.CORBA.Object o) {
        String sior = this._orb.object_to_string(o);
        String t = new cossak.util.IorPrinter(sior).getTypeId();
        return (t);
    }

    public void print_ior(String sior) {
        String tmp = new cossak.util.IorPrinter(sior).Describe();
        String tmp2 = tmp.replaceAll("[;\\[\\]]", "\n");
        MsgBox.show("IOR info", tmp2, true);
    }

    public boolean ping_object(String sior) {
        org.omg.CORBA.Object obj = _orb.string_to_object(sior);
        boolean ret = false;
        try {
            obj._is_a("IDL/MaximumBogosityDude");
            ret = true;
        } catch (Exception ex) {
            ret = false;
        }
        return (ret);
    }

    public boolean unregister_object(String parent_naming_context_ior, String obj_ior, NameComponent[] comp) {
        try {
            org.omg.CORBA.Object o = this._orb.string_to_object(parent_naming_context_ior);
            org.omg.CosNaming.NamingContext nc = null;
            try {
                nc = org.omg.CosNaming.NamingContextHelper.narrow(o);
            } catch (Exception e) {
                MsgBox.show("Error", "Object cannot be unregistered (error narrowing naming context)");
                return (false);
            }
            o = _orb.string_to_object(obj_ior);
            try {
                nc.unbind(comp);
            } catch (Exception e) {
                MsgBox.show("Error", "Unable to unregister name.");
                return (false);
            }
        } catch (Exception e) {
            MsgBox.show("Error", "Unable to unregister name.");
            return (false);
        }
        return (true);
    }

    public boolean register_object(String parent_naming_context_ior, String name, String kind, String obj_ior, boolean should_overwrite) {
        try {
            org.omg.CORBA.Object o = this._orb.string_to_object(parent_naming_context_ior);
            org.omg.CosNaming.NamingContext nc = null;
            try {
                nc = org.omg.CosNaming.NamingContextHelper.narrow(o);
            } catch (Exception e) {
                MsgBox.show("Error", "You must click on a NamingContext (not a final object) in NameService Browser tree \nbefore trying to register an object, so that we know where to register the new name.");
                return (false);
            }
            org.omg.CosNaming.NameComponent comp[] = new org.omg.CosNaming.NameComponent[1];
            comp[0] = new org.omg.CosNaming.NameComponent();
            comp[0].id = name;
            comp[0].kind = kind;
            o = _orb.string_to_object(obj_ior);
            if (should_overwrite) {
                try {
                    nc.unbind(comp);
                } catch (Exception e) {
                }
            }
            nc.bind(comp, o);
        } catch (Exception e) {
            MsgBox.show("Error", "The name you chose is already in use, and you did not choose to overwrite this name.");
            return (false);
        }
        return (true);
    }
}
