@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='com.google.guava', module='guava', version='27.1-jre')

import com.google.common.base.CharMatcher
import org.yaml.snakeyaml.Yaml

def profile = new Yaml().load(new File("expanded_terms.yaml").text)

def out = new File('/home/reality/dist/filesOBO/HPOdictold.txt').text

profile.each { term, syns ->
  syns.each { syn ->
    if(CharMatcher.ascii().matchesAllOf(syn)) {
      out += '\n'+syn+' '+term
    }
  } 
}

new File('new_dict.txt').text = out
