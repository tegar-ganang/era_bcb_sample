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
public class NameServiceBrowser {

    public String _nameservices[];

    DefaultTreeModel _mdl;

    NameServiceTreeNode _rootnode;

    org.omg.CORBA.ORB _orb;

    public java.util.HashMap _hashmap = new java.util.HashMap();

    public rthreadpool _tp = new rthreadpool();

    /** Creates a new instance of NameServiceBrowser */
    public NameServiceBrowser(org.omg.CORBA.ORB orb, DefaultTreeModel mdl, String corbalocs) {
        _tp.add_threads(10);
        _nameservices = corbalocs.split(",");
        _mdl = mdl;
        _orb = orb;
        _rootnode = new NameServiceTreeNode(new String("NameServices"));
        _mdl.setRoot(_rootnode);
    }

    public NameServiceBrowser(org.omg.CORBA.ORB orb, DefaultTreeModel mdl, String[] corbalocs) {
        _tp.add_threads(10);
        _nameservices = corbalocs;
        _mdl = mdl;
        _orb = orb;
        _rootnode = new NameServiceTreeNode(new String("NameServices"));
        _mdl.setRoot(_rootnode);
    }

    public void clear() {
        for (int i = 0; i < _rootnode.getChildCount(); i++) _mdl.removeNodeFromParent((NameServiceTreeNode) _mdl.getChild(_rootnode, i));
        _hashmap = new java.util.HashMap();
    }

    private class namingcontext_tpwork implements java.lang.Runnable {

        public NameServiceTreeNode _currnode;

        public NamingContext _nc;

        public NameServiceBrowser _parent;

        namingcontext_tpwork(NameServiceTreeNode currnode, NamingContext nc, NameServiceBrowser parent) {
            _currnode = currnode;
            _nc = nc;
            _parent = parent;
        }

        public void run() {
            _parent.add_naming_context(_currnode, _nc);
        }
    }

    public void sweep() {
        clear();
        NamingContext nc;
        for (int i = 0; i < _nameservices.length; i++) {
            try {
                org.omg.CORBA.Object o = _orb.string_to_object(_nameservices[i]);
                nc = NamingContextExtHelper.narrow(o);
                NameServiceTreeNode tnode = new NameServiceTreeNode(_nameservices[i]);
                _mdl.insertNodeInto(tnode, _rootnode, 0);
                tnode._ior = _nameservices[i];
                namingcontext_tpwork w = new namingcontext_tpwork(tnode, nc, this);
                _tp.add_work(w);
            } catch (Exception ex) {
                MsgBox.show("Error", "Error contacting NamingService: " + _nameservices[i]);
                ex.printStackTrace();
            }
        }
        _mdl.reload();
    }

    public void add_naming_context(NameServiceTreeNode currnode, NamingContext nc) {
        BindingListHolder bl = new BindingListHolder();
        BindingIteratorHolder bi = new BindingIteratorHolder();
        nc.list(1000, bl, bi);
        Binding bindings[] = bl.value;
        if (bindings.length == 0) return;
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
                tnode._ior = objStr;
                _hashmap.put(tnode._ior, tnode);
                tnode._quickname = pretty_name;
                org.omg.CORBA.Object obj2 = _orb.string_to_object(newnode._objstr);
                String final_type = this.get_type_string(obj2);
                IDLTypePluginMap.DiscoverDeepInterfaces(obj2, final_type);
                _mdl.insertNodeInto(tnode, currnode, 0);
                if (bindings[j].binding_type == BindingType.ncontext) {
                    NamingContext subnc = NamingContextHelper.narrow(obj);
                    newnode._ping_status = 0;
                    namingcontext_tpwork w = new namingcontext_tpwork(tnode, subnc, this);
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