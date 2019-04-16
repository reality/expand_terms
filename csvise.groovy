@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

def profile = new Yaml().load(new File("expanded_terms.yaml").text)
def out = "term\tsynonym\tcorrect?\n"

profile.each { name, syns ->
  syns.each { syn ->
    out += name+'\t'+syn+'\t\n'
  } 
}

new File('validation.csv').text = out
