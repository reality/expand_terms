@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

def terms = []

new File('stand-off/').eachFile { f ->
  f.splitEachLine('\t') { line ->
    def x = line[1].tokenize('|')
    def hpo = x[0].trim()
    def label = x[1].trim().toLowerCase()

    if(!terms.find { it.term == hpo }) {
      terms << [
        name: label,
        term: hpo
      ]
    }
  }
}

println terms.size()
new Yaml().dump(terms, new FileWriter('hpo_terms.yaml'))
