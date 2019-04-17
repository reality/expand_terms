def output = ''

new File('text/').eachFile { f ->
  output += f.getName().tokenize('.')[0] + '\n' + f.text + '\n\n'
}

new File('corpus.txt').text = output
