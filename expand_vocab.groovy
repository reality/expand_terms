@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.codehaus.gpars', module='gpars', version='1.2.1')

import org.yaml.snakeyaml.Yaml
import groovyx.net.http.HTTPBuilder
import groovy.transform.Field
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import groovyx.gpars.GParsPool

def profile = new Yaml().load(new File("${args[0]}").text)
@Field def BANNED_ONTOLOGIES = [ 'GO-PLUS', 'MONDO', 'CCONT', 'jp/bio', 'phenX', 'ontoparonmed' ]
@Field def BANNED_SYNONYMS = [
                    "europe pmc",
                    "kegg compound",
                    "chemidplus",
                    "lipid maps",
                    "beilstein",
                    "reaxys",
                    "nist chemistry webbook", "cas registry number", "lipid maps instance", "beilstein registry number" ]
//@Field def BANNED_ONTOLOGIES = []
//@Field def BANNED_SYNONYMS = []

def terms = [:]
def strip(iri) {
  iri.replaceAll('<', '').replaceAll('>', '')
}
def getSynonyms(http, term, cb) {
  http.get(path: '/api/querynames/', query: [ query: term ]) { resp, json ->
    cb(json.collectEntries { 
      def res = it[1].collect { x ->
        x.label = x.label.collect { l -> l.toLowerCase() }

        if(x.containsKey('synonyms')) {
          x.synonyms = x.synonyms.collect { l -> l.toLowerCase() }
          if((x.label.contains(term) || x.synonyms.contains(term)) && !BANNED_ONTOLOGIES.any { o -> x.ontology.indexOf(o) != -1 || x.class.indexOf(o) != -1 }) {
            x.label + x.synonyms
          }
        } else {
          if(x.label.contains(term) && !BANNED_ONTOLOGIES.any { o -> x.ontology.indexOf(o) != -1 || x.class.indexOf(o) != -1 }) {
            x.label
          }
        }
      }
      res.removeAll([null])
      if(res.size() > 0) { 
        [(strip(it[0])): res]
      } else {
        [:]
      }
    })
  }
}
def getSubclasses(http, iri, cb) {
  http.get(path: '/api/backend/', query: [ script: 'runQuery.groovy', type: 'equivalent', query: iri ]) { resp, json ->
    cb(json.result.collect { 
      if(it) {
      if(!BANNED_ONTOLOGIES.any { o -> it.ontology.indexOf(o) != -1 || it.class.indexOf(o) != -1 }) {
//        [it.label].collect { x -> x + ' ' + it.class } + it.synonyms.collect { x->x+' '+it.class} + it.hasExactSynonym.collect {x->x+' '+it.class} + it.alternative_term.collect { x->x+' '+it.class}
          [it.label] + it.synonyms + it.hasExactSynonym + it.alternative_term
      }
      }
    }.flatten())
  }
}

def fCount = 0
GParsPool.withPool(2) { p ->
  profile.eachParallel { term ->
def http = new HTTPBuilder('http://aber-owl.net/')
    getSynonyms(http, term.name, { syns -> 
      terms[term.name] = syns 
      println "(${fCount}/${profile.size()})"
      fCount++
    })
  }
}

new Yaml().dump(terms, new FileWriter("save.yaml"))

def zCount = 0
GParsPool.withPool(2) { p ->
  terms.eachParallel { name, cls ->
def http = new HTTPBuilder('http://aber-owl.net/')
    if(cls && !profile.find { it.name == name }.containsKey('strict')) { 
      cls.each { iri, syn -> 
          getSubclasses(http, iri, { ts ->
            cls[iri] = cls[iri] + ts
        }) 
      }
    }
    println "(${zCount}/${terms.size()})"
    zCount++
  }
}

println 'collecting...'
def finalTerms = terms.collectEntries { term, cls ->
  [(term): cls.findAll { c, v -> 
            v != false 
          }.collect { c, v -> 
            v.flatten().findAll { 
              it != null
            }.collect{ 
              it.toLowerCase()// + " (${c})" //DEBUG
            } 
          }.flatten().unique(false).findAll { 
            it.indexOf(term) == -1 && it.replaceAll("\\P{InBasic_Latin}", "").size() > 2 && it.indexOf(':') == -1 && it.indexOf('_') == -1 && !BANNED_SYNONYMS.any{ s -> it.indexOf(s) != -1 }
          }
  ]
}

new Yaml().dump(finalTerms, new FileWriter("expanded_terms.yaml"))

