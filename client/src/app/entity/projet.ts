class Projet {

  nom: string = '';
  description: string = '';
  repertoire: string = '';
  fichierPom: string = '';
  packageJson: string = '';
  goMod: string = '';
  cargoToml: string = '';
  parent: Artifact | null = null;
  artifact: Artifact | null = null;
}
