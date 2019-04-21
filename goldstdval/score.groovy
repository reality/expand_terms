def goldStandard = [:]

new File('stand-off/').eachFile { f ->
  goldStandard[f.getName()] = []
  f.splitEachLine('\t') { line ->
    def hpo = line[1].tokenize('|')[0].trim()
    goldStandard[f.getName()] << hpo
  }
}

def evalAnnotations(goldStandard, filename) {
  def tagged = [:]
  new File(filename).text.split('\n\n').each {
    def lines = it.tokenize('\n')
    if(lines.size() == 0) { return }
    def pmid = lines[0]
    lines.removeAt(0)
    tagged[pmid] = lines.collect { l ->
      'HP_'+l.split(';')[0]
    }
  }

  def tp = 0
  def fp = 0
  def fn = 0
  
  tagged.each { pmid, codes ->
    codes.each { code ->
      if(goldStandard[pmid].contains(code)) {
        tp++ 
      } else {
        fp++
      }
    }
  }

  goldStandard.each { pmid, codes ->
    codes.each { code ->
      if(!tagged.containsKey(pmid)) {
//        fn++
      } else if(!tagged[pmid].contains(code)) {
        fn++
      }
    }
  }

  def precision = tp / (tp + fp)
  def recall = tp / (tp + fn)
  def fScore = 2 * (( precision * recall) / (precision + recall))

  println filename + ' metrics: '
  println '  precision: ' + precision
  println '  recall: ' + recall
  println '  f-score: ' + fScore
  println ''
}

evalAnnotations(goldStandard, 'baselineCONCEPTS.csv')
evalAnnotations(goldStandard, 'true_baselineCONCEPTS.csv')
evalAnnotations(goldStandard, 'newbaseCONCEPTS.csv')
//evalAnnotations(goldStandard, 'smallersubCONCEPTS.csv')
evalAnnotations(goldStandard, 'origwithnonlatinCONCEPTS.csv')

