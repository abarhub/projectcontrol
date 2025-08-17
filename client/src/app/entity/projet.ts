import {Artifact} from './artifact';
import {ProjetNode} from './projet-node';
import {ProjetGit} from './projet-git';

export class Projet {

  nom: string = '';
  description: string = '';
  repertoire: string = '';
  fichierPom: string = '';
  packageJson: string = '';
  goMod: string = '';
  cargoToml: string = '';
  parent: Artifact | null = null;
  artifact: Artifact | null = null;
  properties: Map<string, string> = new Map<string, string>();
  dependencies: Artifact[] = [];
  projetEnfants: Projet[] = [];
  modules: string[] = [];
  projetNode: ProjetNode | null = null;
  infoGit: ProjetGit | null = null;

}
