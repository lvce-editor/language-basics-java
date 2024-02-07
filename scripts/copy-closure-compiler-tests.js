import { execaCommand } from 'execa'
import { readdir, readFile, rm, writeFile } from 'node:fs/promises'
import path, { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')

const REPO = 'https://github.com/google/closure-compiler'
const COMMIT = '23e25dae584075decfd7c7e3ecc876cd8f55d42d'

const getTestName = (line) => {
  return (
    'closure-compiler-' +
    line
      .toLowerCase()
      .trim()
      .replaceAll(' ', '-')
      .replaceAll('/', '-')
      .replaceAll(',', '')
      .replaceAll('_', '-')
      .replaceAll('-src-com', '')
      .replaceAll('.java', '')
      .replaceAll('-com-google-gwt-sample', '')
      .replaceAll('-java-com', '')
      .replaceAll('-google', '')
      .replaceAll('-dev', '')
      .replaceAll('-gwt-core', '')
      .replaceAll('-com', '')
      .replaceAll('-user', '')
      .replaceAll('-test-test', 'test')
  )
}

const getAllTests = async (folder) => {
  const dirents = await readdir(folder, { recursive: true })
  const allTests = []
  for (const dirent of dirents) {
    if (!dirent.endsWith('.java')) {
      continue
    }
    const filePath = `${folder}/${dirent}`
    const fileContent = await readFile(filePath, 'utf8')
    allTests.push({
      testContent: fileContent,
      testName: getTestName(dirent),
    })
  }
  return allTests
}

const writeTestFiles = async (allTests) => {
  for (const test of allTests) {
    await writeFile(
      `${root}/test/cases/${test.testName}.java`,
      test.testContent,
    )
  }
}

const main = async () => {
  process.chdir(root)
  await rm(`${root}/.tmp`, { recursive: true, force: true })
  await execaCommand(`git clone ${REPO} .tmp/closure-compiler`)
  process.chdir(`${root}/.tmp/closure-compiler`)
  await execaCommand(`git checkout ${COMMIT}`)
  process.chdir(root)
  const allTests = await getAllTests(`${root}/.tmp/closure-compiler`)
  await writeTestFiles(allTests)
}

main()
