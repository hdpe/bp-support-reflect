/*
 * Copyright 2014 Black Pepper Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.blackpepper.support.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.objenesis.ObjenesisStd;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public final class AnnotationAccessor {
	
	public interface InvocationInfo {

		Method getInvokedMethod();
	}
	
	private static class InvocationInfoMethodInterceptor implements MethodInterceptor, InvocationInfo {
		
		private Method method;

		@Override
		public Object intercept(Object instance, Method method, Object[] params, MethodProxy methodProxy)
			throws NoSuchMethodException {

			if (InvocationInfo.class.getMethod("getInvokedMethod").equals(method)) {
				return getInvokedMethod();
			}
			
			this.method = method;
						
			Class<?> returnType = method.getReturnType();
			return enhance(returnType, this);
		}

		@Override
		public Method getInvokedMethod() {
			return method;
		}
	}
	
	private AnnotationAccessor() {
		throw new AssertionError();
	}

	public static <A extends Annotation> A annotation(Class<A> annotationType, Object on) {
		return ((InvocationInfo) on).getInvokedMethod().getAnnotation(annotationType);
	}

	public static <T> T on(Class<T> clazz) {
		return enhance(clazz, new InvocationInfoMethodInterceptor());
	}
	
	private static <T> T enhance(Class<T> clazz, MethodInterceptor interceptor) {
		Enhancer enhancer = new Enhancer();
		
		if (clazz.isInterface()) {
			enhancer.setInterfaces(new Class<?>[] {InvocationInfo.class, clazz});
		}
		else {
			enhancer.setInterfaces(new Class<?>[] {InvocationInfo.class});
			enhancer.setSuperclass(clazz);
		}

		enhancer.setCallbackType(MethodInterceptor.class);
		
		Factory factory = (Factory) new ObjenesisStd(true).newInstance(enhancer.createClass());
		factory.setCallbacks(new Callback[] {interceptor});
		return (T) factory;
	}
}
