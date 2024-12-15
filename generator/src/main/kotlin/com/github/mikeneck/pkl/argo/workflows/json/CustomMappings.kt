package com.github.mikeneck.pkl.argo.workflows.json

import com.github.mikeneck.pkl.argo.workflows.core.Definition

data class CustomMappings(
    val mappings: Set<Mapping>,
) {

    constructor(vararg mappings: Mapping) : this(mappings.toSet())

  operator fun contains(pair: Pair<String, Body>): Boolean =
      mappings.any { it.matches(pair.first, pair.second) }

  operator fun get(pair: Pair<String, Body>): Definition =
      mappings.first { it.matches(pair.first, pair.second) }.map(pair.first, pair.second)
}