import { execaCommand } from 'execa'
import { cp, readFile, rm, writeFile } from 'node:fs/promises'
import path, { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')

const REPO = 'https://github.com/atom/language-java'
const COMMIT = '047fd33bd12f4926bc5dbc0c930cd9d5fa280602'

const getTestName = (line) => {
  return (
    'atom-language-java-' +
    line
      .toLowerCase()
      .trim()
      .replace("it '", '')
      .replace(`', ->`, '')
      .replaceAll(' ', '-')
      .replaceAll('/', '-')
      .replaceAll('+', '')
      .replaceAll('`', '')
      .replaceAll('--', '-')
  )
}

const deindent = (line) => {
  return line.slice(4)
}

const getTestContent = (lines) => {
  return lines.map(deindent).join('\n').trim() + '\n'
}

const tokenizeLinePrefix = `grammar.tokenizeLine '`
const tokenizeLinesPrefix = `grammar.tokenizeLines '''`

const parseFile = (content) => {
  const tests = []
  const lines = content.split('\n')
  let state = 'top'
  let testName = ''
  let testLines = []
  let index = 1
  for (const line of lines) {
    switch (state) {
      case 'top':
        if (line.trim().startsWith("it '")) {
          testName = getTestName(line)
          state = 'top'
          index = 1
        } else if (line.includes(tokenizeLinePrefix)) {
          state = 'top'
          const startIndex =
            line.indexOf(tokenizeLinePrefix) + tokenizeLinePrefix.length
          const endIndex = -1
          const testContent = line.slice(startIndex, endIndex)
          if (!testName) {
            throw new Error('test name must be defined')
          }
          tests.push({
            testName: `${testName}-${index++}`,
            testContent: testContent + '\n',
          })
          testLines = []
        } else if (line.includes(tokenizeLinesPrefix)) {
          state = 'code-multi-line'
        } else if (line.trim().startsWith('expect(')) {
          state = 'top'
        }
        break
      case 'code-multi-line':
        if (line.trim() === `'''`) {
          state = 'top'
          if (!testName) {
            throw new Error('test name must be defined')
          }
          tests.push({
            testName: `${testName}-${index++}`,
            testContent: getTestContent(testLines),
          })
          testLines = []
        } else {
          testLines.push(line)
        }
        break
      default:
        break
    }
  }
  return tests
}

const getAllTests = async (file) => {
  const allTests = []
  const fileContent = await readFile(file, 'utf8')
  const parsed = parseFile(fileContent)
  allTests.push(...parsed)
  return allTests
}

const writeTestFiles = async (allTests) => {
  for (const test of allTests) {
    await writeFile(
      `${root}/test/cases/${test.testName}.java`,
      test.testContent
    )
  }
}

const main = async () => {
  process.chdir(root)
  await rm(`${root}/.tmp`, { recursive: true, force: true })
  await execaCommand(`git clone ${REPO} .tmp/atom-language-java`)
  process.chdir(`${root}/.tmp/atom-language-java`)
  await execaCommand(`git checkout ${COMMIT}`)
  process.chdir(root)
  await cp(
    `${root}/.tmp/atom-language-java/spec`,
    `${root}/.tmp/atom-language-java-cases`,
    { recursive: true }
  )
  const allTests = await getAllTests(
    `${root}/.tmp/atom-language-java-cases/java-spec.coffee`
  )
  await writeTestFiles(allTests)
}

main()
