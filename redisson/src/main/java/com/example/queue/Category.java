package com.example.queue;

import lombok.Getter;

@Getter
public enum Category {

    /**
     * By default, the redisson RScoredSortedSetReactive
     * is sorted in ascending order
     * Set the highest priority with lowest score
     */
    PRIME(0),
    STD(50),
    GUEST(100);

    private final int score;

    Category(int score) {
        this.score = score;
    }
}
