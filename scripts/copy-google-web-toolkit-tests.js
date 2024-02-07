import { execaCommand } from 'execa'
import { readdir, readFile, rm, writeFile } from 'node:fs/promises'
import path, { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')

const REPO = 'https://github.com/gwtproject/gwt'
const COMMIT = '89c3aaf68588cad317432b3a77f6b8c5a222696b'

const getTestName = (line) => {
  return (
    'google-web-toolkit-' +
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
  await execaCommand(`git clone ${REPO} .tmp/google-web-toolkit`)
  process.chdir(`${root}/.tmp/google-web-toolkit`)
  await execaCommand(`git checkout ${COMMIT}`)
  process.chdir(root)
  const allTests = await getAllTests(`${root}/.tmp/google-web-toolkit`)
  await writeTestFiles(allTests)
}

main()
