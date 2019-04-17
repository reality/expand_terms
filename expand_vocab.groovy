@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import org.yaml.snakeyaml.Yaml
import groovyx.net.http.HTTPBuilder
import groovy.transform.Field

def http = new HTTPBuilder('http://aber-owl.net/')
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

def terms = [:]
def strip(iri) {
  iri.replaceAll('<', '').replaceAll('>', '')
}
def getSynonyms(http, term, cb) {
  http.get(path: '/api/querynames/', query: [ query: term ]) { resp, json ->
    cb(json.collectEntries { 
      def res = it[1].collect { x ->
        x.label = x.label.collect { l -> l.toLowerCase() }

        if(x.label.contains(term) /*&& !BANNED_ONTOLOGIES.any { o -> x.ontology == o || x.class.indexOf(o) != -1 }*/) {
          x.containsKey('synonyms') ? x.synonyms + x.label : x.label
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
  http.get(path: '/api/backend/', query: [ script: 'runQuery.groovy', type: 'subeq', query: iri ]) { resp, json ->
    cb(json.result.collect { 
      if(it) {
      //if(!BANNED_ONTOLOGIES.any { o -> it.ontology == o || it.class.indexOf(o) != -1 }) {
        [it.label] + it.synonyms + it.hasExactSynonym + it.alternative_term 
      //}
      }
    }.flatten())
  }
}

profile.each { term ->
  getSynonyms(http, term.name, { syns -> 
    terms[term.name] = syns 
  })
}

terms.each { name, cls ->
  if(!profile.find { it.name == name }.containsKey('strict')) { 
    cls.each { iri, syn -> 
        getSubclasses(http, iri, { ts ->
          cls[iri] = cls[iri] + ts
      }) 
    }
  }
}

def finalTerms = terms.collectEntries { term, cls ->
  [(profile.find { it.name == term }.term): cls.findAll { c, v -> 
            v != false 
          }.collect { c, v -> 
            v.flatten().findAll { 
              it != null
            }.collect{ 
              it.toLowerCase() //+ " (${c})" DEBUG
            } 
          }.flatten().unique(false).findAll { 
            it.indexOf(term) == -1 && it.indexOf(':') == -1 && it.indexOf('_') == -1 // && !BANNED_SYNONYMS.any{ s -> it.indexOf(s) != -1 }
          } + term 
  ]
}

new Yaml().dump(finalTerms, new FileWriter("expanded_terms.yaml"))
