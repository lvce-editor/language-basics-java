import {
  initialLineState,
  tokenizeLine,
  TokenType,
  TokenMap,
} from '../src/tokenizeJava.js'

const DEBUG = true

const expectTokenize = (text, state = initialLineState.state) => {
  const lineState = {
    state,
  }
  const tokens = []
  const lines = text.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const result = tokenizeLine(lines[i], lineState)
    lineState.state = result.state
    tokens.push(...result.tokens.map((token) => token.type))
    tokens.push(TokenType.NewLine)
  }
  tokens.pop()
  return {
    toEqual(...expectedTokens) {
      if (DEBUG) {
        expect(tokens.map((token) => TokenMap[token])).toEqual(
          expectedTokens.map((token) => TokenMap[token])
        )
      } else {
        expect(tokens).toEqual(expectedTokens)
      }
    },
  }
}

test('empty', () => {
  expectTokenize(``).toEqual()
})

test('whitespace', () => {
  expectTokenize(' ').toEqual(TokenType.Whitespace)
})

test('keyword', () => {
  // see https://en.wikipedia.org/wiki/List_of_Java_keywords
  expectTokenize('_').toEqual(TokenType.Keyword)
  expectTokenize('abstract').toEqual(TokenType.Keyword)
  expectTokenize('assert').toEqual(TokenType.Keyword)
  expectTokenize('boolean').toEqual(TokenType.Keyword)
  expectTokenize('break').toEqual(TokenType.Keyword)
  expectTokenize('byte').toEqual(TokenType.Keyword)
  expectTokenize('case').toEqual(TokenType.Keyword)
  expectTokenize('catch').toEqual(TokenType.Keyword)
  expectTokenize('char').toEqual(TokenType.Keyword)
  expectTokenize('class').toEqual(TokenType.Keyword)
  expectTokenize('const').toEqual(TokenType.Keyword)
  expectTokenize('continue').toEqual(TokenType.Keyword)
  expectTokenize('default').toEqual(TokenType.Keyword)
  expectTokenize('do').toEqual(TokenType.Keyword)
  expectTokenize('double').toEqual(TokenType.Keyword)
  expectTokenize('else').toEqual(TokenType.Keyword)
  expectTokenize('enum').toEqual(TokenType.Keyword)
  expectTokenize('extends').toEqual(TokenType.Keyword)
  expectTokenize('final').toEqual(TokenType.Keyword)
  expectTokenize('finally').toEqual(TokenType.Keyword)
  expectTokenize('float').toEqual(TokenType.Keyword)
  expectTokenize('for').toEqual(TokenType.Keyword)
  expectTokenize('goto').toEqual(TokenType.Keyword)
  expectTokenize('if').toEqual(TokenType.Keyword)
  expectTokenize('implements').toEqual(TokenType.Keyword)
  expectTokenize('import').toEqual(TokenType.Keyword)
  expectTokenize('instanceof').toEqual(TokenType.Keyword)
  expectTokenize('int').toEqual(TokenType.Keyword)
  expectTokenize('interface').toEqual(TokenType.Keyword)
  expectTokenize('long').toEqual(TokenType.Keyword)
  expectTokenize('native').toEqual(TokenType.Keyword)
  expectTokenize('new').toEqual(TokenType.Keyword)
  expectTokenize('package').toEqual(TokenType.Keyword)
  expectTokenize('private').toEqual(TokenType.Keyword)
  expectTokenize('protected').toEqual(TokenType.Keyword)
  expectTokenize('public').toEqual(TokenType.Keyword)
  expectTokenize('return').toEqual(TokenType.Keyword)
  expectTokenize('short').toEqual(TokenType.Keyword)
  expectTokenize('static').toEqual(TokenType.Keyword)
  expectTokenize('super').toEqual(TokenType.Keyword)
  expectTokenize('switch').toEqual(TokenType.Keyword)
  expectTokenize('synchronized').toEqual(TokenType.Keyword)
  expectTokenize('this').toEqual(TokenType.Keyword)
  expectTokenize('throw').toEqual(TokenType.Keyword)
  expectTokenize('throws').toEqual(TokenType.Keyword)
  expectTokenize('transient').toEqual(TokenType.Keyword)
  expectTokenize('try').toEqual(TokenType.Keyword)
  expectTokenize('void').toEqual(TokenType.Keyword)
  expectTokenize('volatile').toEqual(TokenType.Keyword)
  expectTokenize('while').toEqual(TokenType.Keyword)
})

test('double quoted string', () => {
  expectTokenize(`"Hello" abc`).toEqual(
    TokenType.Punctuation,
    TokenType.String,
    TokenType.Punctuation,
    TokenType.Whitespace,
    TokenType.VariableName
  )
})

test('single quoted string', () => {
  expectTokenize(`'Hello' abc`).toEqual(
    TokenType.Punctuation,
    TokenType.String,
    TokenType.Punctuation,
    TokenType.Whitespace,
    TokenType.VariableName
  )
})
