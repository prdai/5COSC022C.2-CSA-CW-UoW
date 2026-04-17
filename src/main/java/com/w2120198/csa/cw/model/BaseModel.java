package com.w2120198.csa.cw.model;

/**
 * Identity contract shared by every stored entity so the generic DAO
 * can key records by id without reflection.
 */
public interface BaseModel {

    String getId();

    void setId(String id);
}
