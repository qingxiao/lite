package org.xidea.el.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.el.ExpressionImpl;

public class CheckTest {
	ExpressionFactory expressionFactory = ExpressionFactoryImpl.getInstance();

	public void invalid(String el) throws Exception {
		try {
			expressionFactory.createEL(el);
			expressionFactory.optimizeEL(el);
			Assert.fail("必须抛ExpressionSyntaxException异常");
		} catch (org.xidea.el.ExpressionSyntaxException e) {
		} catch (java.lang.AssertionError e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			Assert.fail("只能是抛ExpressionSyntaxException异常");
		}
	}

	@Test
	public void testEL1() throws Exception {
		invalid("1+");
		invalid("1;+1");

	}
}