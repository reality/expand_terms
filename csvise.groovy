@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='com.google.guava', module='guava', version='27.1-jre')
import org.yaml.snakeyaml.Yaml
import com.google.common.base.CharMatcher

def profile = new Yaml().load(new File("expanded_terms.yaml").text)
def out = "term\tsynonym\tcorrect?\n"

profile.each { name, syns ->
  syns.each { syn ->
    if(syn.replaceAll("\\P{InBasic_Latin}", "").size() > 2) {
      out += name+'\t'+syn+'\t\n'
    }
  } 
}

new File('validation.csv').text = out
