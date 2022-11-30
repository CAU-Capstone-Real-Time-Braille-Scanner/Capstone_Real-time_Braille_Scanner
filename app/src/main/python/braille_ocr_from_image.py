# -*- coding: utf-8 -*-
"""
Created on Thu Nov 24 19:11:15 2022

@author: 노현진
"""

import os
os.environ['KMP_DUPLICATE_LIB_OK']='True'

from pathlib import Path
import local_config
import model.infer_retinanet as infer_retinanet
from BrailleToKor import BrailleToKor

recognizer = ""

def loadModel():
    global recognizer
    model_weights = 'model.t7'
    recognizer = infer_retinanet.BrailleInference(
            params_fn=os.path.join(local_config.data_path, 'weights', 'param.txt'),
            model_weights_fn=os.path.join(local_config.data_path, 'weights', model_weights),
            create_script=None)

def getBrailleText(path):
    global recognizer
    results_dir = Path(path).parent
    result = recognizer.run_and_getBrailleText(path, results_dir, target_stem=None,
                                           lang='EN', extra_info=None,
                                           draw_refined=recognizer.DRAW_NONE,
                                           remove_labeled_from_filename=False,
                                           find_orientation=False,
                                           align_results=True,
                                           process_2_sides=False,
                                           repeat_on_aligned=False,
                                           save_development_info=False)
    if result is None:
        return ['인식불가']
    else:
        translatedResult = []
        for line in result:
            translatedResult.append(BrailleToKor().translation(line))
        return result, translatedResult
