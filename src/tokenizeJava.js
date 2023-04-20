/**
 * @enum number
 */
const State = {
  None: 0,
  TopLevelContent: 1,
  InsideSingleQuoteString: 2,
  InsideDoubleQuoteString: 3,
  AfterKeywordClass: 4,
  InsideBlockComment: 5,
}

/**
 * @enum number
 */
export const TokenType = {
  Whitespace: 0,
  None: 1,
  Keyword: 2,
  NewLine: 3,
  VariableName: 4,
  Punctuation: 5,
  String: 6,
  Numeric: 7,
  Attribute: 8,
  KeywordControl: 9,
  KeywordReturn: 10,
  KeywordNew: 11,
  KeywordThis: 12,
  Class: 13,
  Comment: 14,
  FunctionName: 885,
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
  [TokenType.KeywordControl]: 'KeywordControl',
  [TokenType.KeywordReturn]: 'KeywordReturn',
  [TokenType.KeywordNew]: 'KeywordNew',
  [TokenType.KeywordThis]: 'KeywordThis',
  [TokenType.Class]: 'Class',
  [TokenType.Comment]: 'Comment',
  [TokenType.FunctionName]: 'Function',
}

export const initialLineState = {
  state: State.TopLevelContent,
}

const RE_KEYWORD =
  /^(?:_|abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\b/

const RE_WHITESPACE = /^\s+/
const RE_VARIABLE_NAME = /^[a-zA-Z\_\$]+/
const RE_PUNCTUATION = /^[:,;\{\}\[\]\.=\(\)>\|\-\*\+\:\<\>]/
const RE_QUOTE_SINGLE = /^'/
const RE_QUOTE_DOUBLE = /^"/
const RE_STRING_SINGLE_QUOTE_CONTENT = /^[^']+/
const RE_STRING_DOUBLE_QUOTE_CONTENT = /^[^"]+/
const RE_NUMERIC = /^\d+/
const RE_TRIPLE_DOUBLE_QUOTE = /^"""/
const RE_STRING_TRIPLE_CONTENT = /^.+?(?="""|$)/s
const RE_LINE_COMMENT = /^\/\/[^\n]*/
const RE_ATTRIBUTE = /^@\w+/
const RE_BLOCK_COMMENT_START = /^\/\*/
const RE_BLOCK_COMMENT_CONTENT = /^.+?(?=\*\/)/
const RE_BLOCK_COMMENT_END = /^\*\//
const RE_CURLY_OPEN = /^\{/
const RE_ANYTHING_UNTIL_END = /^.+/s
const RE_SLASH = /^\//
const RE_FUNCTION_CALL_NAME = /^[\w]+(?=\s*(\())/

export const hasArrayReturn = true

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
          switch (next[0]) {
            case 'as':
            case 'switch':
            case 'default':
            case 'case':
            case 'else':
            case 'if':
            case 'break':
            case 'throw':
            case 'for':
            case 'try':
            case 'catch':
            case 'finally':
            case 'continue':
            case 'while':
              token = TokenType.KeywordControl
              break
            case 'return':
              token = TokenType.KeywordReturn
              break
            case 'new':
              token = TokenType.KeywordNew
              break
            case 'this':
              token = TokenType.KeywordThis
              break
            case 'class':
              token = TokenType.Keyword
              state = State.AfterKeywordClass
              break
            default:
              token = TokenType.Keyword
              break
          }
        } else if ((next = part.match(RE_FUNCTION_CALL_NAME))) {
          token = TokenType.FunctionName
          state = State.TopLevelContent
        } else if ((next = part.match(RE_SLASH))) {
          if ((next = part.match(RE_BLOCK_COMMENT_START))) {
            token = TokenType.Comment
            state = State.InsideBlockComment
          } else if ((next = part.match(RE_LINE_COMMENT))) {
            token = TokenType.Comment
            state = State.TopLevelContent
          } else {
            next = part.match(RE_SLASH)
            token = TokenType.Punctuation
            state = State.TopLevelContent
          }
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
      case State.AfterKeywordClass:
        if ((next = part.match(RE_WHITESPACE))) {
          token = TokenType.Whitespace
          state = State.AfterKeywordClass
        } else if ((next = part.match(RE_VARIABLE_NAME))) {
          token = TokenType.Class
          state = State.TopLevelContent
        } else if ((next = part.match(RE_LINE_COMMENT))) {
          token = TokenType.Comment
          state = State.TopLevelContent
        } else if ((next = part.match(RE_BLOCK_COMMENT_START))) {
          token = TokenType.Comment
          state = State.InsideBlockComment
        } else if ((next = part.match(RE_CURLY_OPEN))) {
          token = TokenType.Punctuation
          state = State.TopLevelContent
        } else {
          throw new Error('no')
        }
        break
      case State.InsideBlockComment:
        if ((next = part.match(RE_BLOCK_COMMENT_END))) {
          token = TokenType.Comment
          state = State.TopLevelContent
        } else if ((next = part.match(RE_BLOCK_COMMENT_CONTENT))) {
          token = TokenType.Comment
          state = State.InsideBlockComment
        } else if ((next = part.match(RE_ANYTHING_UNTIL_END))) {
          token = TokenType.Comment
          state = State.InsideBlockComment
        } else {
          throw new Error('no')
        }
        break
      default:
        state
        throw new Error('no')
    }
    const currentTokenText = next[0]
    const currentTokenLength = currentTokenText.length
    index += currentTokenLength
    tokens.push(token, currentTokenLength)
  }
  return {
    state,
    tokens,
  }
}
