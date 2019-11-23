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
		if (null != method.getAnnotation(Select.class)) {// 判断该方法上是否有Select注解  下同
			Select select = method.getAnnotation(Select.class); // 获得注解
			String sql = select.value();// 获得注解中的value值
			if (args == null) {// 如果args为空 执行selectAll 如果不为空处理参数执行select
				return db.findMutil(sql, null, getGenericReturnType(method));
			}
			String[] sqls = sql.split("}");// } 分割sql语句将#{ } 替换成 ?
			String trueSql = ""; // 处理完的sql
			for (String str : sqls) {
				// 找到# 找到}
				int start = str.indexOf("#");
				str = str.substring(0, start) + "?";
				trueSql += str;
			}
			List<?> result=db.findMutil(trueSql, args, method.getReturnType());
			if(result.size()==1) {
				return result.get(0);
			}
			return result;
		} else if (null != method.getAnnotation(Update.class)) {// 同上
			Update update = method.getAnnotation(Update.class);// 同上
			String sql = update.value();// 同上
			String[] sqls = sql.split("}");// } 分割sql语句将#{ } 替换成 ?
			String trueSql = "";// 处理玩的sql
			List<String> paramType=new ArrayList<String>();// Sql语句中的参数名列表
			for (int i = 0; i < sqls.length; i++) {// 找到# 之后留下#之前的东西--》age=  然后拼接上？--》age=？
				int start = sqls[i].indexOf("#");
				paramType.add(sqls[i].substring(start+2));// 将sql语句中参数放置到参数列表中
				sqls[i] = sqls[i].substring(0, start) + "?";
				trueSql += sqls[i];
			}
			List<Object> pa=new ArrayList<Object>();// 初始参数列表
			Class<?> [] clss= method.getParameterTypes();
			for(int i=0;i<clss.length;i++) {// 循环参数类型数组  目前仅限于一种参数类型而且是对象(类似于Student这种)
				pa=(List<Object>) createParams(paramType,clss[i], args[i]);
			}
			System.out.println(trueSql);
			int i=0;
			for (Object p : pa) {// 获得数组长度（最终参数列表长度）
				if(p!=null) {
					i++;
				}
			}
			Object [] params=new Object[i];
			for(i=0;i<params.length;i++) {// 获得最终参数列表
				params[i]=pa.get(i);
			}
			return db.update(trueSql, params);
		}
		return null;
	}
	
	/**
	 * 设置参数
	 * @param paramType		sql语句中的参数列表
	 * @param cls			方法参数类型（对象）
	 * @param o				方法传入的参数（对象）
	 * @return
	 */
	private <E>List<E> createParams(List<String> paramType,Class<E> cls,Object o){
		List<E> list=new ArrayList<E>();
		Method[] methods = cls.getDeclaredMethods();// 获得所有的方法
		for(String f:paramType) {
			for (Method method : methods) {
				if(("get"+f).equalsIgnoreCase(method.getName())) { // 获得get方法
					try {
						list.add((E) method.invoke(o, null));// 执行get方法，并把参数放置到list集合中
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
