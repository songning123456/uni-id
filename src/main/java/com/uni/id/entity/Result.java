package com.uni.id.entity;

import com.uni.id.eum.Status;
import lombok.Data;

/**
 * @author songning
 * @date 2020/5/25
 * description
 */
@Data
public class Result {
    private long id;
    private Status status;

    public Result(long id, Status status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public String toString() {
        return "Result{" + "id=" + id + ", status=" + status + '}';
    }
}
