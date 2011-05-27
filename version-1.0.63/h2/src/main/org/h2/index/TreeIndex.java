/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.sql.SQLException;

import org.h2.constant.SysProperties;
import org.h2.engine.Session;
import org.h2.message.Message;
import org.h2.result.Row;
import org.h2.result.SearchRow;
import org.h2.table.IndexColumn;
import org.h2.table.TableData;
import org.h2.value.Value;
import org.h2.value.ValueNull;


public class TreeIndex extends BaseIndex {

    private TreeNode root;
    private TableData tableData;

    public TreeIndex(TableData table, int id, String indexName, IndexColumn[] columns, IndexType indexType) {
        super(table, id, indexName, columns, indexType);
        tableData = table;
    }

    public void close(Session session) throws SQLException {
        root = null;
    }

    public void add(Session session, Row row) throws SQLException {
        TreeNode i = new TreeNode(row);
        TreeNode n = root, x = n;
        boolean isLeft = true;
        while (true) {
            if (n == null) {
                if (x == null) {
                    root = i;
                    rowCount++;
                    return;
                }
                set(x, isLeft, i);
                break;
            }
            Row r = n.row;
            int compare = compareRows(row, r);
            if (compare == 0) {
                if (indexType.isUnique()) {
                    if (!isNull(row)) {
                        throw getDuplicateKeyException();
                    }
                }
                compare = compareKeys(row, r);
            }
            isLeft = compare < 0;
            x = n;
            n = child(x, isLeft);
        }
        balance(x, isLeft);
        rowCount++;
    }

    private void balance(TreeNode x, boolean isLeft) {
        while (true) {
            int sign = isLeft ? 1 : -1;
            switch (x.balance * sign) {
            case 1:
                x.balance = 0;
                return;
            case 0:
                x.balance = -sign;
                break;
            case -1:
                TreeNode l = child(x, isLeft);
                if (l.balance == -sign) {
                    replace(x, l);
                    set(x, isLeft, child(l, !isLeft));
                    set(l, !isLeft, x);
                    x.balance = 0;
                    l.balance = 0;
                } else {
                    TreeNode r = child(l, !isLeft);
                    replace(x, r);
                    set(l, !isLeft, child(r, isLeft));
                    set(r, isLeft, l);
                    set(x, isLeft, child(r, !isLeft));
                    set(r, !isLeft, x);
                    int rb = r.balance;
                    x.balance = (rb == -sign) ? sign : 0;
                    l.balance = (rb == sign) ? -sign : 0;
                    r.balance = 0;
                }
                return;
            default:
                throw Message.getInternalError("b: " + x.balance * sign);
            }
            if (x == root) {
                return;
            }
            isLeft = x.isFromLeft();
            x = x.parent;
        }
    }

    private TreeNode child(TreeNode x, boolean isLeft) {
        return isLeft ? x.left : x.right;
    }

    private void replace(TreeNode x, TreeNode n) {
        if (x == root) {
            root = n;
            if (n != null) {
                n.parent = null;
            }
        } else {
            set(x.parent, x.isFromLeft(), n);
        }
    }

    private void set(TreeNode parent, boolean left, TreeNode n) {
        if (left) {
            parent.left = n;
        } else {
            parent.right = n;
        }
        if (n != null) {
            n.parent = parent;
        }
    }

    public void remove(Session session, Row row) throws SQLException {
        TreeNode x = findFirstNode(row, true);
        if (x == null) {
            throw Message.getInternalError("not found!");
        }
        TreeNode n;
        if (x.left == null) {
            n = x.right;
        } else if (x.right == null) {
            n = x.left;
        } else {
            TreeNode d = x;
            x = x.left;
            for (TreeNode temp = x; (temp = temp.right) != null;) {
                x = temp;
            }
            // x will be replaced with n later
            n = x.left;
            // swap d and x
            int b = x.balance;
            x.balance = d.balance;
            d.balance = b;

            // set x.parent
            TreeNode xp = x.parent;
            TreeNode dp = d.parent;
            if (d == root) {
                root = x;
            }
            x.parent = dp;
            if (dp != null) {
                if (dp.right == d) {
                    dp.right = x;
                } else {
                    dp.left = x;
                }
            }
            // TODO index / tree: link d.r = x(p?).r directly
            if (xp == d) {
                d.parent = x;
                if (d.left == x) {
                    x.left = d;
                    x.right = d.right;
                } else {
                    x.right = d;
                    x.left = d.left;
                }
            } else {
                d.parent = xp;
                xp.right = d;
                x.right = d.right;
                x.left = d.left;
            }

            if (SysProperties.CHECK && (x.right == null || x == null)) {
                throw Message.getInternalError("tree corrupted");
            }
            x.right.parent = x;
            x.left.parent = x;
            // set d.left, d.right
            d.left = n;
            if (n != null) {
                n.parent = d;
            }
            d.right = null;
            x = d;
        }
        rowCount--;

        boolean isLeft = x.isFromLeft();
        replace(x, n);
        n = x.parent;
        while (n != null) {
            x = n;
            int sign = isLeft ? 1 : -1;
            switch (x.balance * sign) {
            case -1:
                x.balance = 0;
                break;
            case 0:
                x.balance = sign;
                return;
            case 1:
                TreeNode r = child(x, !isLeft);
                int b = r.balance;
                if (b * sign >= 0) {
                    replace(x, r);
                    set(x, !isLeft, child(r, isLeft));
                    set(r, isLeft, x);
                    if (b == 0) {
                        x.balance = sign;
                        r.balance = -sign;
                        return;
                    }
                    x.balance = 0;
                    r.balance = 0;
                    x = r;
                } else {
                    TreeNode l = child(r, isLeft);
                    replace(x, l);
                    b = l.balance;
                    set(r, isLeft, child(l, !isLeft));
                    set(l, !isLeft, r);
                    set(x, !isLeft, child(l, isLeft));
                    set(l, isLeft, x);
                    x.balance = (b == sign) ? -sign : 0;
                    r.balance = (b == -sign) ? sign : 0;
                    l.balance = 0;
                    x = l;
                }
                break;
            default:
                throw Message.getInternalError("b: " + x.balance * sign);
            }
            isLeft = x.isFromLeft();
            n = x.parent;
        }
    }

    private TreeNode findFirstNode(SearchRow row, boolean withKey) throws SQLException {
        TreeNode x = root, result = x;
        while (x != null) {
            result = x;
            int compare = compareRows(x.row, row);
            if (compare == 0 && withKey) {
                compare = compareKeys(x.row, row);
            }
            if (compare == 0) {
                if (withKey) {
                    return x;
                }
                x = x.left;
            } else if (compare > 0) {
                x = x.left;
            } else {
                x = x.right;
            }
        }
        return result;
    }

    public Cursor find(Session session, SearchRow first, SearchRow last) throws SQLException {
        if (first == null) {
            TreeNode x = root, n;
            while (x != null) {
                n = x.left;
                if (n == null) {
                    break;
                }
                x = n;
            }
            return new TreeCursor(this, x, null, last);
        } else {
            TreeNode x = findFirstNode(first, false);
            return new TreeCursor(this, x, first, last);
        }
    }

    public int getLookupCost(int rowCount) {
        for (int i = 0, j = 1;; i++) {
            j += j;
            if (j >= rowCount) {
                return i;
            }
        }
    }

    public double getCost(Session session, int[] masks) throws SQLException {
        return getCostRangeIndex(masks, tableData.getRowCount(session));
    }

    public void remove(Session session) throws SQLException {
        truncate(session);
    }

    public void truncate(Session session) throws SQLException {
        root = null;
        rowCount = 0;
    }

    TreeNode next(TreeNode x) {
        if (x == null) {
            return null;
        }
        TreeNode r = x.right;
        if (r != null) {
            x = r;
            TreeNode l = x.left;
            while (l != null) {
                x = l;
                l = x.left;
            }
            return x;
        }
        TreeNode ch = x;
        x = x.parent;
        while (x != null && ch == x.right) {
            ch = x;
            x = x.parent;
        }
        return x;
    }

    public void checkRename() throws SQLException {
    }

    public boolean needRebuild() {
        return true;
    }

    public boolean canGetFirstOrLast() {
        return true;
    }

    public SearchRow findFirstOrLast(Session session, boolean first) throws SQLException {
        if (first) {
            // TODO optimization: this loops through NULL values
            Cursor cursor = find(session, null, null);
            while (cursor.next()) {
                SearchRow row = cursor.getSearchRow();
                Value v = row.getValue(columnIds[0]);
                if (v != ValueNull.INSTANCE) {
                    return row;
                }
            }
            return null;
        } else {
            TreeNode x = root, n;
            while (x != null) {
                n = x.right;
                if (n == null) {
                    break;
                }
                x = n;
            }
            if (x != null) {
                return x.row;
            }
            return null;
        }
    }

}