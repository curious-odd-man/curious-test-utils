package com.github.curiousoddman.curioustestutils.populate.impl;

import com.github.curiousoddman.curioustestutils.populate.PopulatePojo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpecificValueGenerators {
	@Builder
	@RequiredArgsConstructor
	public static class FilledListGenerator implements ValueGenerator<List<?>> {
		private final int maxListElements;
		private final Class<?> elementType;

		@Override
		@SneakyThrows
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			Class<?> genericType = context.getCurrentGenericType();
			return parameterClass.isAssignableFrom(List.class) && genericType.isAssignableFrom(elementType);
		}

		@Override
		public List<?> generateValue(ReadOnlyContext context) {
			int elementCount = context.getSeed() % maxListElements;
			return IntStream
					.range(0, elementCount)
					.mapToObj(i -> new Context(context.getSeed() + i, context.getPathElements(), context.getCustomGenerators()))
					.map(ctx -> PopulatePojo.populatePojo(elementType, ctx))
					.collect(Collectors.toList());
		}
	}
}
