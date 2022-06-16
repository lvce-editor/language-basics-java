/**
 * @enum number
 */
const State = {
  None: 0,
  TopLevelContent: 1,
  InsideSingleQuoteString: 2,
  InsideDoubleQuoteString: 3,
}

/**
 * @enum number
 */
export const TokenType = {
  None: 99999999,
  Keyword: 951,
  Whitespace: 0,
  NewLine: 771,
  VariableName: 2,
  Punctuation: 3,
  String: 4,
  Numeric: 5,
  Attribute: 6,
}

export const TokenMap = {
  [TokenType.None]: 'None',
  [TokenType.Keyword]: 'Keyword',
  [TokenType.Whitespace]: 'Whitespace',
  [TokenType.NewLine]: 'NewLine',
  [TokenType.VariableName]: 'VariableName',
  [TokenType.Punctuation]: 'Punctuation',
  [TokenType.String]: 'String',
  [TokenType.Numeric]: 'Numeric',
  [TokenType.Attribute]: 'Attribute',
}

export const initialLineState = {
  state: State.TopLevelContent,
}

const RE_KEYWORD =
  /^(?:_|abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\b/
const RE_WHITESPACE = /^\s+/
const RE_VARIABLE_NAME = /^[a-zA-Z]+/
const RE_PUNCTUATION = /^[:,;\{\}\[\]\.=\(\)>]/
const RE_QUOTE_SINGLE = /^'/
const RE_QUOTE_DOUBLE = /^"/
const RE_STRING_SINGLE_QUOTE_CONTENT = /^[^']+/
const RE_STRING_DOUBLE_QUOTE_CONTENT = /^[^"]+/
const RE_NUMERIC = /^\d+/
const RE_TRIPLE_DOUBLE_QUOTE = /^"""/
const RE_STRING_TRIPLE_CONTENT = /^.+?(?="""|$)/s
const RE_ATTRIBUTE = /^@\w+/

/**
 * @param {string} line
 * @param {any} lineState
 */
export const tokenizeLine = (line, lineState) => {
  let next = null
  let index = 0
  let tokens = []
  let token = TokenType.None
  let state = lineState.state
  while (index < line.length) {
    const part = line.slice(index)
    switch (state) {
      case State.TopLevelContent:
        if ((next = part.match(RE_WHITESPACE))) {
          token = TokenType.Whitespace
          state = State.TopLevelContent
        } else if ((next = part.match(RE_KEYWORD))) {
          token = TokenType.Keyword
          state = State.TopLevelContent
        } else if ((next = part.match(RE_VARIABLE_NAME))) {
          token = TokenType.VariableName
          state = State.TopLevelContent
        } else if ((next = part.match(RE_PUNCTUATION))) {
          token = TokenType.Punctuation
          state = State.TopLevelContent
        } else if ((next = part.match(RE_NUMERIC))) {
          token = TokenType.Numeric
          state = State.TopLevelContent
        } else if ((next = part.match(RE_QUOTE_SINGLE))) {
          token = TokenType.Punctuation
          state = State.InsideSingleQuoteString
        } else if ((next = part.match(RE_QUOTE_DOUBLE))) {
          token = TokenType.Punctuation
          state = State.InsideDoubleQuoteString
        } else if ((next = part.match(RE_ATTRIBUTE))) {
          token = TokenType.Attribute
          state = State.TopLevelContent
        } else {
          part //?
          throw new Error('no')
        }
        break
      case State.InsideSingleQuoteString:
        if ((next = part.match(RE_QUOTE_SINGLE))) {
          token = TokenType.Punctuation
          state = State.TopLevelContent
        } else if ((next = part.match(RE_STRING_SINGLE_QUOTE_CONTENT))) {
          token = TokenType.String
          state = State.InsideSingleQuoteString
        } else {
          throw new Error('no')
        }
        break
      case State.InsideDoubleQuoteString:
        if ((next = part.match(RE_QUOTE_DOUBLE))) {
          token = TokenType.Punctuation
          state = State.TopLevelContent
        } else if ((next = part.match(RE_STRING_DOUBLE_QUOTE_CONTENT))) {
          token = TokenType.String
          state = State.InsideDoubleQuoteString
        } else {
          throw new Error('no')
        }
        break
      default:
        state
        throw new Error('no')
    }
    index += next[0].length
    tokens.push({
      type: token,
      length: next[0].length,
    })
  }
  return {
    state,
    tokens,
  }
}
