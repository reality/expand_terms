@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovyx.net.http.HTTPBuilder
import groovy.transform.Field
import org.yaml.snakeyaml.Yaml

def parentClass = args[0]
def trainCount = args[1].toInteger()
def testCount = args[2].toInteger()

def http = new HTTPBuilder('http://aber-owl.net/')
def terms = [:]
def getSubclasses(http, iri, cb) {
  http.get(path: '/api/backend/', query: [ 
      script: 'runQuery.groovy', 
      type: 'subclass', 
      query: iri,
      ontology: 'HP',
      type: 'subclass' ]) { resp, json ->
    cb(json.result)  
  }
}

getSubclasses(http, parentClass, { cls ->
  Collections.shuffle(cls)
  println cls.size()
  terms['train'] = cls.subList(0, trainCount).collect { [ name: it.label.toLowerCase(), term: it.class ] }
  terms['test'] = cls.subList(trainCount, trainCount+testCount).collect { [ name: it.label.toLowerCase(), term: it.class ] }
})

new Yaml().dump(terms['train'], new FileWriter('train_terms.yaml'))
new Yaml().dump(terms['test'], new FileWriter('test_terms.yaml'))
