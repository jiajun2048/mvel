package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;

/**
 * 使用数组+下标来存储变量值的变量解析器
 * 变量在存储时声明相应在数组中的下标，后续即可根据此下标进行数据操作和处理
 */
public class IndexVariableResolver implements VariableResolver {
  /** 相应的下标位置 */
  private int indexPos;
  /** 存储数据的数组 */
  private Object[] vars;

  public IndexVariableResolver(int indexPos, Object[] vars) {
    this.indexPos = indexPos;
    this.vars = vars;
  }

  /** 没有变量名 */
  public String getName() {
    return null;
  }

  /** 没有变量类型 */
  public Class getType() {
    return null;
  }

  public void setStaticType(Class type) {
  }

  /** 没有特殊标记 */
  public int getFlags() {
    return 0;
  }

  public Object getValue() {
    return vars[indexPos];
  }

  public void setValue(Object value) {
    vars[indexPos] = value;
  }
}
