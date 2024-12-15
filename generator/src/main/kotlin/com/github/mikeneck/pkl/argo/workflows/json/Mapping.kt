package com.github.mikeneck.pkl.argo.workflows.json

import com.github.mikeneck.pkl.argo.workflows.core.Definition

interface Mapping {
  fun matches(name: String, body: Body): Boolean
  fun map(name: String, body: Body): Definition

  class ByName(private val name: String, private  val mapping: Body.(String) -> Definition) : Mapping {
      override fun matches(name: String, body: Body): Boolean = name == this.name
      override fun map(name: String, body: Body): Definition = body.mapping(name)
  }
}