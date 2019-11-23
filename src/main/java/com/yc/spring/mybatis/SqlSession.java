package com.yc.spring.mybatis;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.yc.spring.anno.Select;
import com.yc.spring.anno.Update;
import com.yc.spring.commons.DbHelper;

public class SqlSession implements InvocationHandler {

	private DbHelper db = new DbHelper();

	@SuppressWarnings("unchecked")
	public <T> T getMapper(Class<T> cls) {
		// 使用 JDK 动态代理创建对象
		return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[] { cls }, this);
	}

	@Override
	/**
	 * 作业： 请完成该方法，实现通过对 method 上注解的读取，完成对应的SQL操作 
	 * 要求： 
	 * 1、根据实体类创建 student表，包含：sn、name、age、grade 4个字段 
	 * 2、使用 DBHelper 执行SQL 
	 * 3、使用反射获取 SQL语句
	 * 4、解析语句中的命名参数，根据参数名从 Java 的方法参数中获取 SQL 参数值 
	 * 5、通过反射获取返回结果类型，将结果集按照返回类型返回 
	 * 6、完成StudentMapperTest.test() 方法的单元测试，并测试通过 
	 * 7、注意：selectAll 返回的是泛型集合，泛型类型请通过下面的getGenericReturnType() 返回获取
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (null != method.getAnnotation(Select.class)) {
			Select select = method.getAnnotation(Select.class);
			String sql = select.value();
			if (args == null) {
				return db.findMutil(sql, null, getGenericReturnType(method));
			}
			String[] sqls = sql.split("}");// , 分割sql语句将#{ } 替换成 ?
			String trueSql = "";
			for (String str : sqls) {
				// 找到# 找到}
				int start = str.indexOf("#");
				str = str.substring(0, start) + "?";
				trueSql += str;
			}
			return db.findMutil(trueSql, args, method.getReturnType()).get(0);
		} else if (null != method.getAnnotation(Update.class)) {
			Update update = method.getAnnotation(Update.class);
			String sql = update.value();
			String[] sqls = sql.split("}");// , 分割sql语句将#{ } 替换成 ?
			String trueSql = "";
			List<String> paramType=new ArrayList<String>();// Sql语句中的参数名列表
			for (int i = 0; i < sqls.length; i++) {
				// 找到# 找到}
				int start = sqls[i].indexOf("#");
				paramType.add(sqls[i].substring(start+2));// 将sql语句中参数放置到参数列表中
				sqls[i] = sqls[i].substring(0, start) + "?";
				trueSql += sqls[i];
			}
			List<Object> pa=new ArrayList<Object>();
			Class<?> [] clss= method.getParameterTypes();
			for(int i=0;i<clss.length;i++) {
				pa=(List<Object>) createParams(paramType,clss[i], args[i]);
			}
			System.out.println(trueSql);
			int i=0;
			for (Object p : pa) {
				if(p!=null) {
					i++;
				}
			}
			Object [] params=new Object[i];
			for(i=0;i<params.length;i++) {
				params[i]=pa.get(i);
			}
			return db.update(trueSql, params);
		}
		return null;
	}
	
	/**
	 * 设置参数
	 * @param cls
	 * @param o
	 * @return
	 */
	private <E>List<E> createParams(List<String> paramType,Class<E> cls,Object o){
		List<E> list=new ArrayList<E>();
		Method[] methods = cls.getDeclaredMethods();
		for(String f:paramType) {
			for (Method method : methods) {
				if(("get"+f).equalsIgnoreCase(method.getName())) {
					try {
						list.add((E) method.invoke(o, null));
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return list;
	}

	/**
	 * 返回一个方法的返回值的泛型类型 例如：将 public List<Student> selectAll(); 方法对象 method 传入，将返回
	 * Student 类对象
	 * 
	 * @param method
	 * @return
	 */
	public static Class<?> getGenericReturnType(Method method) {
		ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
		try {
			return Class.forName(type.getActualTypeArguments()[0].getTypeName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
