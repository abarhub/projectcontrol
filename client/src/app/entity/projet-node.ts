export class ProjetNode {

  nom: string = '';
  version: string = '';
  script: Map<String, String> | null = null;
  dependencies: Map<String, String> | null = null;
  devDependencies: Map<String, String> | null = null;
}
