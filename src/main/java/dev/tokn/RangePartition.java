package dev.tokn;

import static js.base.Tools.*;

import java.util.List;

import static dev.tokn.ToknUtils.*;

/**
 * A data structure that transforms a set of CodeSets to a disjoint set of them,
 * such that no two range sets overlap.
 * 
 * This is to improve the efficiency of the NFA => DFA algorithm, which involves
 * gathering information about what states are reachable on certain characters.
 * We can't afford to treat each character as a singleton, since the ranges can
 * be quite large. Hence, we want to treat ranges of characters as single
 * entities; this will only work if no two such ranges overlap.
 * 
 * It works by starting with a tree whose node is labelled with the maximal
 * superset of character values. Then, for each edge in the NFA, performs a DFS
 * on this tree, splitting any node that only partially intersects any one set
 * that appears in the edge label. The running time is O(n log k), where n is
 * the size of the NFA, and k is the height of the resulting tree.
 * 
 * We encourage k to be small by sorting the NFA edges by their label
 * complexity.
 */
public class RangePartition {

  /**
   * A node within a RangePartition tree
   */
  private static class RPNode {
    public RPNode(  CodeSet codeSet) {
      this.codeSet = codeSet;
    }

    CodeSet codeSet;
    List<RPNode> children = arrayList();
  }

  private boolean mPrepared;
  private RPNode mRootNode;
  private List<CodeSet> mSetsToAdd;

  public RangePartition() {
    mSetsToAdd = arrayList();

    //  Make the root node hold the largest possible CodeSet. 
    // We want to be able to include all the token ids as well.
    mRootNode = buildNode(CodeSet.withRange(CODEMIN, CODEMAX));
    // Add epsilon immediately, so it's always in its own subset
    addSet(CodeSet.withValue(EPSILON));
  }

  public void addSet(CodeSet codeSet) {
    checkState(!mPrepared);

    // If set already exists, omit
    for (CodeSet s : mSetsToAdd) {
      if (equal(s, codeSet))
        return;
    }

    push(mSetsToAdd, codeSet);
  }

  //
  //    def initialize()
  //      # We will build a tree, where each node has a CodeSet
  //      # associated with it, and the child nodes (if present)
  //      # partition this CodeSet into smaller, nonempty sets.
  //
  //      # A tree is represented by a node, where each node is a pair [x,y],
  //      # with x the node's CodeSet, and y a list of the node's children.
  //
  //      @nextNodeId = 0
  //      @prepared = false
  //
  //      # Make the root node hold the largest possible CodeSet.
  //      # We want to be able to include all the token ids as well.
  //
  //      @rootNode = buildNode(CodeSet.new(CODEMIN,CODEMAX))
  //
  //      @setsToAdd = Set.new
  //
  //      # Add epsilon immediately, so it's always in its own subset
  //      addSet(CodeSet.new(EPSILON))
  //    end
  //

  //
  public void prepare() {
    checkState(!mPrepared);
    // Construct partition from previously added sets

    //
    //      list = @setsToAdd.to_a
    //
    // Sort set by cardinality: probably get a more balanced tree
    // if larger sets are processed first
    mSetsToAdd.sort((a, b) -> Integer.compare(b.elements().length, a.elements().length));

    for (CodeSet s : mSetsToAdd) {
      addSetAux(s, mRootNode);
    }
    mPrepared = true;
  }

  /**
   * Apply the partition to a CodeSet
   * 
   * @param s
   *          CodeSet
   * @return array of subsets from the partition whose union equals s (this
   *         array will be the single element s if no partitioning was
   *         necessary)
   */
  public List<CodeSet> apply(CodeSet s) {
    checkState(mPrepared);
    List<CodeSet> list = arrayList();
    CodeSet s2 = s.dup(); // Not sure this is necessary
    applyAux(mRootNode, s2, list);

    // Sort the list of subsets by their first elements
    list.sort((a, b) -> Integer.compare(a.elements()[0], b.elements()[0]));
    return list;
  }

  private void applyAux(RPNode n, CodeSet s, List<CodeSet> list) {
    if (n.children.isEmpty()) {
      // Verify that this set equals the input set
      checkState(equal(s, n.codeSet));
      push(list, s);
    } else {
      for (RPNode m : n.children) {
        CodeSet s1 = s.intersect(m.codeSet);
        if (s1.isEmpty())
          continue;
        applyAux(m, s1, list);
        s = s.difference(m.codeSet);
        if (s.isEmpty())
          break;
      }

    }
  }

  //
  private RPNode buildNode(CodeSet codeSet) {
    return new RPNode(  codeSet);
  }

  //
  //    def buildNodeList(list, root = nil)
  //      if not root
  //        root = @rootNode
  //      end
  //      list.push(root)
  //      root.children.each do |x|
  //        buildNodeList(list, x)
  //      end
  //    end
  //

  /**
   * Add a set to the tree, extending the tree as necessary to maintain a
   * (disjoint) partition
   */
  private void addSetAux(CodeSet s, RPNode n) {

    //      #
    //      # The algorithm is this:
    //      #
    //      # add (s, n)    # add set s to node n; s must be subset of n.set
    //      #   if n.set = s, return
    //      #   if n is leaf:
    //      #     x = n.set - s
    //      #     add x,y as child sets of n
    //      #   else
    //      #     for each child m of n:
    //      #       t = intersect of m.set and s
    //      #       if t is nonempty, add(t, m)
    //      #
    if (equal(n.codeSet, s))
      return;

    if (n.children.isEmpty()) {
      //    if n.children.empty?
      CodeSet x = n.codeSet.difference(s);
      push(n.children, buildNode(x));
      push(n.children, buildNode(s));

    } else {
      for (RPNode m : n.children) {
        CodeSet t = m.codeSet.intersect(s);
        if (!t.isEmpty()) {
          addSetAux(t, m);
        }
      }
    }
  }

}
