package com.sk.learn.hash;


import ao.ai.ml.model.input.RealList;

public interface SemanticHasher<T>
{
    RealList hash(T hasher);
}
