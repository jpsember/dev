package dev.tokn;

import js.data.DataUtil;
import js.data.IntArray;
import js.json.JSList;

import static js.base.Tools.*;

import java.util.Arrays;

import static dev.tokn.ToknUtils.*;

public final class CodeSet {

  public static CodeSet withValue(int value) {
    todo("This class can be reduced to an int array (or a 'range') for simplicity");
    CodeSet c = new CodeSet();
    c.add(value);
    return c;
  }

  public static CodeSet withRange(int min, int maxPlusOne) {
    CodeSet c = new CodeSet();
    c.add(min, maxPlusOne);
    return c;
  }

  public static CodeSet with(int[] elements) {
    CodeSet c = new CodeSet();
    c.withElem(elements);
    return c;
  }

  public CodeSet dup() {
    return with(mElements);
  }

  public void add(int value) {
    add(value, value + 1);
  }

  /**
   * Return the single value represented by the set
   */
  public int single_value() {
    if (mElements.length == 2) {
      int a = mElements[0];
      int b = mElements[1];
      if (b == a + 1)
        return a;
    }
    throw badArg("CodeSet does not contain exactly one value");
  }

  /**
   * Add every value from another CodeSet to this one
   * 
   */
  public void addSet(int[] sa) {
    for (int i = 0; i < sa.length; i += 2) {
      add(sa[i], sa[i + 1]);
    }
  }

  /**
   * Add every value from another CodeSet to this one
   * 
   */
  public void addSet(CodeSet s) {
    addSet(s.elements());
  }

  /**
   * Add a contiguous range of values to the set
   * 
   * @param lower
   *          minimum value in range
   * @param upper
   *          one plus maximum value in range
   */
  public void add(int lower, int upper) {
    checkArgument(lower < upper);
    IntArray.Builder new_elements = IntArray.newBuilder();

    int i = 0;
    while (elem(i, lower) < lower) {
      new_elements.add(elem(i));
      i++;
    }

    if ((i & 1) == 0) {
      new_elements.add(lower);
    }

    while (elem(i, upper) < upper)
      i++;

    if ((i & 1) == 0)
      new_elements.add(upper);

    while (i < mElements.length) {
      new_elements.add(elem(i));
      i++;
    }
    withElem(new_elements.build().array());
  }

  private int elem(int index) {
    return mElements[index];
  }

  private int elem(int index, int defaultValue) {
    if (index >= mElements.length)
      return defaultValue;
    return elem(index);
  }

  /**
   * Remove a contiguous range of values from the set
   */
  public void remove(int lower, int upper) {
    checkArgument(lower < upper);
    IntArray.Builder new_elements = IntArray.newBuilder();

    int i = 0;
    while (elem(i, lower) < lower) {
      new_elements.add(elem(i));
      i++;
    }

    if ((i & 1) == 1)
      new_elements.add(lower);

    while (elem(i, upper + 1) <= upper)
      i++;

    if ((i & 1) == 1)
      new_elements.add(upper);

    while (i < mElements.length) {
      new_elements.add(mElements[i]);
      i++;
    }

    withElem(new_elements.build().array());
  }

  private void withElem(int[] elem) {
    if ((elem.length & 1) != 0) {
      badArg("odd number of elements:", elem);
    }
    mElements = elem;
    m__hashcode = 0;
  }

  public void setTo(CodeSet source) {
    withElem(source.mElements);
  }

  /**
   * Calculate difference of this set minus another
   */
  public CodeSet difference(CodeSet s) {
    return combineWith(s, false);
  }

  /** Calculate the intersection of this set and another */
  public CodeSet intersect(CodeSet s) {
    return combineWith(s, true);
  }

  /**
   * Negate the inclusion of a contiguous range of values [lower..upper)
   */
  public CodeSet negate(int lower, int upper) {
    checkArgument(lower < upper);
    IntArray.Builder new_elements = IntArray.newBuilder();
    int i = 0;
    while (elem(i, lower + 1) <= lower) {
      new_elements.add(elem(i));
      i++;
    }
    if (i > 0 && new_elements.get(i - 1) == lower)
      pop(new_elements);
    else
      new_elements.add(lower);

    while (elem(i, upper + 1) <= upper) {
      new_elements.add(elem(i));
      i++;
    }

    if (new_elements.size() > 0 && last(new_elements) == upper)
      pop(new_elements);
    else
      new_elements.add(upper);

    while (i < mElements.length) {
      new_elements.add(elem(i));
      i++;
    }

    CodeSet ret = new CodeSet();
    ret.withElem(new_elements.array());
    return ret;
  }

  private static int last(IntArray.Builder ia) {
    return ia.get(ia.size() - 1);
  }

  private static void pop(IntArray.Builder ia) {
    ia.remove(ia.size() - 1);
  }

  public static boolean contains(int[] rangePairs, int val) {
    int i = 0;
    int[] e = rangePairs;
    while (i < e.length) {
      if (val < e[i])
        return false;
      if (val < e[i + 1])
        return true;
      i += 2;
    }
    return false;
  }

  public boolean contains(int val) {
    return contains(mElements, val);
  }

  /**
   * Get JSON respresentation, which could be a list, or a scalar value
   */
  public Object toJson() {
    IntArray.Builder z = IntArray.newBuilder();
    for (int index = 0; index < mElements.length; index += 2) {
      int a = mElements[index];
      int b = mElements[index + 1];
      if (b == a + 1) {
        z.add(a);
      } else {
        if (b == CODEMAX)
          b = 0;
        z.add(a);
        z.add(b);
      }
    }
    if (z.size() == 1)
      return z.get(0);
    return JSList.with(z.array());
  }

  /**
   * Construct result of combining this CodeSet with another
   * 
   * @param code_set
   * @param intersect
   *          if true, returns this ^ code_set; otherwise, returns this -
   *          code_set
   */
  private CodeSet combineWith(CodeSet code_set, boolean intersect) {

    CodeSet sa = this;
    CodeSet sb = code_set;

    int i = 0;
    int j = 0;
    IntArray.Builder combined_elements = IntArray.newBuilder();

    boolean was_inside = false;

    //pr("combine_with:",INDENT,sa.mElements,INDENT,sb.mElements,"intersect?",intersect);

    while (i < sa.mElements.length || j < sb.mElements.length) {

      int v;
      if (i == sa.mElements.length)
        v = sb.mElements[j];
      else if (j == sb.mElements.length)
        v = sa.mElements[i];
      else
        v = Math.min(sa.mElements[i], sb.mElements[j]);

      if (i < sa.mElements.length && v == sa.mElements[i])
        i++;
      if (j < sb.mElements.length && v == sb.mElements[j])
        j++;

      boolean inside;
      if (intersect)
        inside = ((i & 1) == 1) && ((j & 1) == 1);
      else
        inside = ((i & 1) == 1) && ((j & 1) == 0);

      if (inside != was_inside) {
        combined_elements.add(v);
        was_inside = inside;
      }
    }
    CodeSet ret = new CodeSet();
    ret.withElem(combined_elements.array());
    return ret;
  }

  @Override
  public String toString() {
    return ToknUtils.dumpCodeRange(mElements);
  }

  private int[] mElements = DataUtil.EMPTY_INT_ARRAY;

  public int[] elements() {
    return mElements;
  }

  public boolean isEmpty() {
    return mElements.length == 0;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CodeSet))
      return false;
    CodeSet other = (CodeSet) object;
    if (other.hashCode() != hashCode())
      return false;

    return Arrays.equals(mElements, other.mElements);
  }

  @Override
  public int hashCode() {
    if (m__hashcode == 0) {
      m__hashcode = 1 + Arrays.hashCode(mElements);
    }
    return m__hashcode;
  }

  private int m__hashcode;
}
