package org.oddlama.vane.core.functional;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public interface GenericsFinder extends Serializable {
	default SerializedLambda serialized() {
		try {
			Method replaceMethod = getClass().getDeclaredMethod("writeReplace");
			replaceMethod.setAccessible(true);
			return (SerializedLambda) replaceMethod.invoke(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	default Class<?> getContainingClass() {
		try {
			String className = serialized().getImplClass().replaceAll("/", ".");
			return Class.forName(className);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	default Method method() {
		SerializedLambda lambda = serialized();
		Class<?> containingClass = getContainingClass();
		return Arrays.stream(containingClass.getDeclaredMethods())
			.filter(method -> Objects.equals(method.getName(), lambda.getImplMethodName()))
			.findFirst()
			.orElseThrow(RuntimeException::new);
	}
}
