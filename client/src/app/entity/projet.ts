import {Artifact} from './artifact';
import {ProjetNode} from './projet-node';
import {ProjetGit} from './projet-git';

export class Projet {

  id: string = '';
  nom: string = '';
  description: string = '';
  repertoire: string = '';
  parent: Artifact | null = null;
  artifact: Artifact | null = null;
  properties: Map<string, string> = new Map<string, string>();
  dependencies: Artifact[] = [];
  projetEnfants: Projet[] = [];
  modules: string[] = [];
  infoNode: ProjetNode | null = null;
  infoGit: ProjetGit | null = null;
  dateModification: Date | null = null;
  detailModules: Map<string, string> = new Map<string, string>();

}
