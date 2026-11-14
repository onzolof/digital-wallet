module.exports = {
  schemaFile: '../../../backend/src/main/resources/openapi/wallet-api.yaml',
  apiFile: './emptyApi.ts',
  apiImport: 'emptySplitApi',
  outputFile: './walletApi.ts',
  exportName: 'walletApi',
  hooks: true,
  prettierConfigFile: '../../.prettierrc.json',
}
