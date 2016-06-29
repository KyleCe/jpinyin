package com.github.stuxuhai.jpinyin;

import org.junit.Assert;
import org.junit.Test;

public class TestPinyinHelper {

    @Test
    public void testGetShortPinyin() {
        Assert.assertEquals(PinyinHelper.getShortPinyin("你好世界"), "nhsj");
        Assert.assertEquals(PinyinHelper.getShortPinyin("智明星通"), "zmxt");
    }
}
