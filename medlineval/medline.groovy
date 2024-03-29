@Grab('org.yaml:snakeyaml:1.17')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovyx.net.http.HTTPBuilder
import org.yaml.snakeyaml.Yaml
import groovy.transform.Field
import groovy.util.XmlSlurper


def allSyns = new Yaml().load(new File("all_syns.yaml").text).collect { k, v -> [ term: k, vals: v ] }
def hpSyns = new Yaml().load(new File("hp_syns.yaml").text).collect { k, v -> [ term: k, vals: v ] }

def getMedlineCount(medline, terms, cb) {
  def searchString = terms.join(' OR ')//.replaceAll(' ', '%20')
  medline.get(path: '/entrez/eutils/esearch.fcgi', query: [ email:'l.slater.1@bham.ac.uk', tool:'synval', db: 'pmc', term: searchString ]) { resp ->
    def body = resp.entity.content.text.replace('<!DOCTYPE eSearchResult PUBLIC "-//NLM//DTD esearch 20060628//EN" "https://eutils.ncbi.nlm.nih.gov/eutils/dtd/20060628/esearch.dtd">','')
    def res = new XmlSlurper().parseText(body)
    cb(res.childNodes()[0].text())
  }
}

allSyns = allSyns.subList(0, 250)
hpSyns = hpSyns.subList(0, 250)

def allCount = 0
allSyns.each { allCount += it.vals.size() }

def hpCount = 0
hpSyns.each { hpCount += it.vals.size() }

println 'All synonyms: ' + allCount
println 'HP synonyms: ' + hpCount

def doTheThing(i, collexion, syns) {
    def medline = new HTTPBuilder('https://eutils.ncbi.nlm.nih.gov/')
  getMedlineCount(medline, syns[i].vals, {
    collexion[syns[i].term] = it.toInteger()

    println i + ': ' + it

    sleep(1200)
    if(i < syns.size()-1) { doTheThing(i+1, collexion, syns) }
    collexion
  })
  collexion
}

def allCounts = doTheThing(0, [:], allSyns)
new Yaml().dump(allCounts, new FileWriter("all_counts.yaml"))

def hpCounts = doTheThing(0, [:], hpSyns)
new Yaml().dump(hpCounts, new FileWriter("hp_counts.yaml"))

def allQueryCount = 0
allCounts.each { k, v -> allQueryCount += v }

def hpQueryCount = 0
hpCounts.each { k, v -> hpQueryCount += v }

println 'Query result ALL: ' + allQueryCount
println 'Query result HP: ' + hpQueryCount
