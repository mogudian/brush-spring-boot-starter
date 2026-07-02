package com.cotti.franchise.tools.brush.model;

import java.io.Serializable;

public class Column implements Serializable {

	private static final long serialVersionUID = -1L;

	private String name;

	private String type;

	private int length;

	private boolean notnull;

	private String comment;

	private boolean auto;

	private boolean pk;
	
	public Column(String name, String type, int length, boolean notnull,
			String comment, boolean auto, boolean pk) {
		super();
		this.name = name;
		this.type = type;
		this.length = length;
		this.notnull = notnull;
		this.comment = comment;
		this.auto = auto;
		this.pk = pk;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || !(o == null || getClass() != o.getClass()) && name.equals(((Column) o).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isNotnull() {
		return notnull;
	}

	public void setNotnull(boolean notnull) {
		this.notnull = notnull;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	public boolean isPk() {
		return pk;
	}

	public void setPk(boolean pk) {
		this.pk = pk;
	}

}
