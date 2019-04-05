@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import org.yaml.snakeyaml.Yaml
import groovyx.net.http.HTTPBuilder

def http = new HTTPBuilder('http://aber-owl.net/')
def profile = new Yaml().load(new File("${args[0]}").text)

def terms = [:]
def strip(iri) {
  iri.replaceAll('<', '').replaceAll('>', '')
}
def getSynonyms(http, term, cb) {
  http.get(path: '/api/querynames/', query: [ query: term ]) { resp, json ->
    cb(json.collectEntries { 
      def res = it[1].collect { x ->
        x.label = x.label.collect { l -> l.toLowerCase() }

        if(x.label.contains(term)) {
          x.containsKey('synonyms') ? x.synonyms + x.label : x.label
        }
      }
      res.removeAll([null])
      if(res.size() != 0) { 
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
      if(!['MONDO'].any { o -> it.class.indexOf(o) != -1 } ) {
        [it.label] + it.synonyms + it.hasExactSynonym
      }
    }.flatten())
  }
}

profile.each { term ->
  getSynonyms(http, term.name, { syn -> terms[term.name] = syn })
}

terms.each { name, cls ->
  cls.each { iri, syn ->
    getSubclasses(http, iri, { ts ->
      cls[iri] = cls[iri] + ts
    }) 
  }
}

def finalTerms = terms.collectEntries { term, cls ->
  [(term): cls.findAll { c, v -> 
            v != false 
          }.collect { c, v -> 
            v.flatten().findAll { 
              it != null 
            }.collect{ 
              it.toLowerCase()
            } 
          }.flatten().unique(false).findAll { 
            it.indexOf(term) == -1 
          } 
  ]
}

new Yaml().dump(finalTerms, new FileWriter("expanded_terms.yaml"))
