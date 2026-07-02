package com.mogudiandian.brush.util;

public final class NameUtils {

	private NameUtils() {
	}

	public static String hungaryToCamelCase(String hungary, boolean isFirstLetterUpperCase) {
		if (hungary == null || hungary.isEmpty()) {
			throw new RuntimeException("Invalid Name");
		}
		StringBuilder temp = new StringBuilder(hungary);
		while (temp.charAt(0) == '_') {
			temp.deleteCharAt(0);
		}
		while (temp.charAt(temp.length() - 1) == '_') {
			temp.deleteCharAt(temp.length() - 1);
		}
		if (temp.length() == 0) {
			throw new RuntimeException("Invalid Name");
		}
		char ch = temp.charAt(0);
		if (!Character.isLetter(ch) && ch != '_' && ch != '$') {
			throw new RuntimeException("Illegal Name");
		}
		temp.append('_');
		int lastIndex = 0;
		StringBuilder buffer = new StringBuilder();
		for (int i = 0, len = temp.length(); i < len; i++) {
			if (temp.charAt(i) == '_') {
				temp.setCharAt(lastIndex, Character.toUpperCase(temp.charAt(lastIndex)));
				buffer.append(temp.substring(lastIndex, i));
				lastIndex = i + 1;
			}
		}
		if (!isFirstLetterUpperCase) {
			buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
		}
		return buffer.toString();
	}

	public static String camelCaseToHungary(String camelCase) {
		if (camelCase == null || camelCase.length() == 0) {
			throw new RuntimeException("Invalid Name");
		}
		StringBuilder temp = new StringBuilder(camelCase);
		temp.setCharAt(0, Character.toLowerCase(temp.charAt(0)));
		temp.append('A');
		int lastIndex = 0;
		StringBuilder buffer = new StringBuilder();
		for (int i = 0, len = temp.length(); i < len; i++) {
			if (Character.isUpperCase(temp.charAt(i))) {
				temp.setCharAt(lastIndex, Character.toLowerCase(temp.charAt(lastIndex)));
				buffer.append(temp.substring(lastIndex, i));
				buffer.append('_');
				lastIndex = i;
			}
		}
		if (buffer.length() > 0) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		return buffer.toString();
	}

}
