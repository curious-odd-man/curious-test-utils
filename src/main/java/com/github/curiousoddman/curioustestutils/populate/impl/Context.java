package com.github.curiousoddman.curioustestutils.populate.impl;

import lombok.Getter;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Getter
public class Context implements ReadOnlyContext {
	private final int seed;
	private final Deque<PathElement> path;
	private final ValueGenerator<?>[] customGenerators;

	public Context(int seed, ValueGenerator<?>... customGenerators) {
		this(seed, new LinkedList<>(), customGenerators);
	}

	public Context(int seed, Deque<PathElement> path, ValueGenerator<?>[] customGenerators) {
		this.seed = seed;
		this.path = new LinkedList<>(path);
		this.customGenerators = customGenerators;
	}

	public void push(String setterName, Class<?> argType, Class<?> argGenericType) {
		path.push(new PathElement(setterName, argType, argGenericType));
	}

	public void pop() {
		path.pop();
	}

	public List<String> getPath() {
		return path.stream().map(PathElement::setterName).toList();
	}

	public Class<?> getCurrentParameterClass() {
		return path.getFirst().type();
	}

	public String getCurrentMethodName() {
		return path.getFirst().setterName();
	}

	public Class<?> getCurrentGenericType() {
		return path.getFirst().genericType();
	}

	public Deque<PathElement> getPathElements() {
		return path;
	}
}
