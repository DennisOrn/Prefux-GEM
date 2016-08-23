# Prefux-GEM

This repository contains two versions of GEM. A touch screen is required to use all the functionality. Jena should be added to the project in Eclipse, the version of Jena that has been tested is 3.1.0.

##Version 1 (Normal)

Everything is expanded at the same time, this is how the algorithm was originally intended to work.

Files:
- GraphEmbedderLayout.java *(Prefux-master/src/main/java/prefux/action/layout/graph/)*
- GemMain.java *(Prefux-master/src/test/java/fx/)*
- GemControl.java *(Prefux-master/src/main/java/prefux/controls/)*
- ArrowRenderer.java *(Prefux-master/src/main/java/prefux/render/)*

##Version 2 (Levels)

A specific number of levels are expanded at first, the user can then expand more nodes by clicking on the nodes that are collapsed. Run time is improved, but the resulting graph typically has more edge crossings. The number of levels is specified in the variable "level" in *GemMain2.java*.

Files:
- GraphEmbedderLayout2.java *(Prefux-master/src/main/java/prefux/action/layout/graph/)*
- GemMain2.java *(Prefux-master/src/test/java/fx/)*
- GemControl2.java *(Prefux-master/src/main/java/prefux/controls/)*
- ArrowRenderer.java *(Prefux-master/src/main/java/prefux/render/)*

**_Remember to specify which ontology-file to read in GemMain.java / GemMain2.java_**
