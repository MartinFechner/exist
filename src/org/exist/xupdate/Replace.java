package org.exist.xupdate;

import java.util.Map;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.AttrImpl;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.ElementImpl;
import org.exist.dom.NodeImpl;
import org.exist.dom.TextImpl;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xquery.XPathException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements xupdate:replace, an extension to the XUpdate standard.
 * The modification replaces a node and its contents. It differs from xupdate:update
 * which only replaces the contents of the node, not the node itself.
 * 
 * @author wolf
 */
public class Replace extends Modification {

	/**
	 * @param broker
	 * @param docs
	 * @param selectStmt
	 * @param namespaces
	 * @param variables
	 */
	public Replace(DBBroker broker, DocumentSet docs, String selectStmt,
			Map namespaces, Map variables) {
		super(broker, docs, selectStmt, namespaces, variables);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xupdate.Modification#process()
	 */
	public long process(Txn transaction) throws PermissionDeniedException, LockException,
			EXistException, XPathException {
		NodeList children = content;
        if (children.getLength() == 0) 
            return 0;
        if (children.getLength() > 1)
        	throw new EXistException("xupdate:replace requires exactly one content node");
        LOG.debug("processing replace ...");
        int modifications = children.getLength();
        try {
            NodeImpl ql[] = selectAndLock();
            IndexListener listener = new IndexListener(ql);
            NodeImpl node;
            Node temp;
            TextImpl text;
            AttrImpl attribute;
            ElementImpl parent;
            DocumentImpl doc = null;
            DocumentSet modifiedDocs = new DocumentSet();
            for (int i = 0; i < ql.length; i++) {
                node = ql[i];
                if (node == null) {
                    LOG.warn("select " + selectStmt + " returned empty node set");
                    continue;
                }
                doc = (DocumentImpl) node.getOwnerDocument();
                doc.setIndexListener(listener);
                modifiedDocs.add(doc);
                if (!doc.getPermissions().validate(broker.getUser(),
                        Permission.UPDATE))
                        throw new PermissionDeniedException(
                                "permission to update document denied");
                parent = (ElementImpl) node.getParentNode();
                if (parent == null)
                    throw new EXistException("The root element of a document can not be replaced with 'xu:replace'. " +
                        "Please consider removing the document or use 'xu:update' to just replace the children of the root.");
                switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        if (modifications == 0) modifications = 1;
                        temp = children.item(0);
                        parent.replaceChild(transaction, temp, node);
                        break;
                    case Node.TEXT_NODE:
                        temp = children.item(0);
                        text = new TextImpl(temp.getNodeValue());
                        modifications = 1;
                        text.setOwnerDocument(doc);
                        parent.updateChild(transaction, node, text);
                        break;
                    case Node.ATTRIBUTE_NODE:
                        AttrImpl attr = (AttrImpl) node;
                        temp = children.item(0);
                        attribute = new AttrImpl(attr.getQName(), temp.getNodeValue());
                        attribute.setOwnerDocument(doc);
                        parent.updateChild(transaction, node, attribute);
                        break;
                    default:
                        throw new EXistException("unsupported node-type");
                }
                doc.setLastModified(System.currentTimeMillis());
                broker.storeDocument(transaction, doc);
            }
            checkFragmentation(transaction, modifiedDocs);
        } finally {
            unlockDocuments();
        }
        return modifications;
	}

	/* (non-Javadoc)
	 * @see org.exist.xupdate.Modification#getName()
	 */
	public String getName() {
		return XUpdateProcessor.REPLACE;
	}

}
