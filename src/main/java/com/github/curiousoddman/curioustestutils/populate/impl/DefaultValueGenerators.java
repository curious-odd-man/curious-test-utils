package com.github.curiousoddman.curioustestutils.populate.impl;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class DefaultValueGenerators {
	public static List<ValueGenerator<?>> getDefaultGenerators() {
		return List.of(
				new IntGenerator(),
				new LongGenerator(),
				new DoubleGenerator(),
				new FloatGenerator(),
				new BooleanGenerator(),
				new StringGenerator(),
				new InstantGenerator(),
				new BigDecimalGenerator(),
				new LocalDateGenerator(),
				new LocalDateTimeGenerator(),
				new MapGenerator(),
				new ListGenerator(),
				new EnumGenerator(),
				new OffsetDateTimeGenerator(),
				new SqlDateGenerator(),
				new UtilDateGenerator(),
				new SqlTimestampGenerator()
		);
	}

	public static class IntGenerator implements ValueGenerator<Integer> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return Stream.of(int.class, Integer.class).anyMatch(parameterClass::isAssignableFrom);
		}

		@Override
		public Integer generateValue(ReadOnlyContext context) {
			return context.getSeed();
		}
	}

	public static class LongGenerator implements ValueGenerator<Long> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return Stream.of(long.class, Long.class).anyMatch(parameterClass::isAssignableFrom);
		}

		@Override
		public Long generateValue(ReadOnlyContext context) {
			return (long) context.getSeed();
		}
	}

	public static class DoubleGenerator implements ValueGenerator<Double> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return Stream.of(double.class, Double.class).anyMatch(parameterClass::isAssignableFrom);
		}

		@Override
		public Double generateValue(ReadOnlyContext context) {
			return (double) context.getSeed();
		}
	}

	public static class FloatGenerator implements ValueGenerator<Float> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return Stream.of(float.class, Float.class).anyMatch(parameterClass::isAssignableFrom);
		}

		@Override
		public Float generateValue(ReadOnlyContext context) {
			return (float) context.getSeed();
		}
	}

	public static class BooleanGenerator implements ValueGenerator<Boolean> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return Stream.of(boolean.class, Boolean.class).anyMatch(parameterClass::isAssignableFrom);
		}

		@Override
		public Boolean generateValue(ReadOnlyContext context) {
			return context.getSeed() % 2 == 0;
		}
	}

	public static class StringGenerator implements ValueGenerator<String> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(String.class);
		}

		@Override
		public String generateValue(ReadOnlyContext context) {
			return context.getCurrentMethodName() + "-String-" + context.getSeed();
		}
	}

	public static class InstantGenerator implements ValueGenerator<Instant> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(Instant.class);
		}

		@Override
		public Instant generateValue(ReadOnlyContext context) {
			return OffsetDateTime.of(2020, 1, 1, 12, 11, 10, 123789, ZoneOffset.UTC).plusDays(context.getSeed()).toInstant();
		}
	}

	public static class BigDecimalGenerator implements ValueGenerator<BigDecimal> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(BigDecimal.class);
		}

		@Override
		public BigDecimal generateValue(ReadOnlyContext context) {
			return BigDecimal.valueOf(context.getSeed());
		}
	}

	public static class LocalDateGenerator implements ValueGenerator<LocalDate> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(LocalDate.class);
		}

		@Override
		public LocalDate generateValue(ReadOnlyContext context) {
			return LocalDate.of(2021, 1, 1).plusDays(context.getSeed());
		}
	}

	public static class LocalDateTimeGenerator implements ValueGenerator<LocalDateTime> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(LocalDateTime.class);
		}

		@Override
		public LocalDateTime generateValue(ReadOnlyContext context) {
			return LocalDateTime.of(2021, 1, 1, 11, 10, 9).plusDays(context.getSeed());
		}
	}

	public static class MapGenerator implements ValueGenerator<Map<?, ?>> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(Map.class);
		}

		@Override
		public Map<?, ?> generateValue(ReadOnlyContext context) {
			return new HashMap<>();
		}
	}

	public static class ListGenerator implements ValueGenerator<List<?>> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(List.class);
		}

		@Override
		public List<?> generateValue(ReadOnlyContext context) {
			return new ArrayList<>();
		}
	}

	public static class EnumGenerator implements ValueGenerator<Object> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isEnum();
		}

		@Override
		public Object generateValue(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			Object[] enumConstants = parameterClass.getEnumConstants();
			return enumConstants[Math.abs(context.getSeed()) % enumConstants.length];
		}
	}

	public static class OffsetDateTimeGenerator implements ValueGenerator<OffsetDateTime> {
		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(OffsetDateTime.class);
		}

		@Override
		public OffsetDateTime generateValue(ReadOnlyContext context) {
			return OffsetDateTime.of(
					LocalDate.ofYearDay(2000, 1),
					LocalTime.of(9, 8, 7, 654321), ZoneOffset.UTC
			).plusDays(context.getSeed());
		}
	}

	public static class SqlDateGenerator implements ValueGenerator<java.sql.Date> {

		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(java.sql.Date.class);
		}

		@Override
		public java.sql.Date generateValue(ReadOnlyContext context) {
			return java.sql.Date.valueOf(LocalDate.of(2021, 2, 3).plusDays(context.getSeed()));
		}
	}

	public static class UtilDateGenerator implements ValueGenerator<java.util.Date> {

		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(java.util.Date.class);
		}

		@Override
		public java.util.Date generateValue(ReadOnlyContext context) {
			Instant instant = OffsetDateTime.of(2022, 1, 1, 12, 11, 10, 123789, ZoneOffset.UTC).plusDays(context.getSeed()).toInstant();
			return java.util.Date.from(instant);
		}
	}

	public static class SqlTimestampGenerator implements ValueGenerator<java.sql.Timestamp> {

		@Override
		public boolean isApplicable(ReadOnlyContext context) {
			Class<?> parameterClass = context.getCurrentParameterClass();
			return parameterClass.isAssignableFrom(java.sql.Timestamp.class);
		}

		@Override
		public java.sql.Timestamp generateValue(ReadOnlyContext context) {
			Instant instant = OffsetDateTime
					.of(2020, 1, 1, 12, 11, 10, 123789, ZoneOffset.UTC)
					.plusDays(context.getSeed())
					.toInstant();
			return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
		}
	}
}
