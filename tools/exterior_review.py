#!/usr/bin/env python3
"""Independent 3D QA for the *real GLB mesh* of a Boeing 747-400.

Axes: X wings, Y up, Z nose. Units: metres. A passing dimension test
is not visual acceptance; the user must approve the actual renderings.
"""
from __future__ import annotations
import argparse, json
from pathlib import Path
import numpy as np
import trimesh

EXPECTED={"span_x_m":64.44,"height_y_m":19.41,"length_z_m":70.66}
VIEWS={
 "01_front":((0,0,150),(0,1,0)),
 "02_side":((150,0,0),(0,1,0)),
 "03_top":((0,150,0),(0,0,-1)),
 "04_front_three_quarter":((110,55,135),(0,1,0)),
 "05_rear_three_quarter":((115,70,-130),(0,1,0)),
 "06_rear":((0,0,-150),(0,1,0)),
}

def read_model(path):
 scene=trimesh.load(path,force="scene",process=False)
 out=[]
 for node in scene.graph.nodes_geometry:
  transform, name=scene.graph[node]
  g=scene.geometry[name]
  if not isinstance(g,trimesh.Trimesh):continue
  c=g.copy();c.apply_transform(transform);out.append((node,c))
 if not out:raise ValueError("No triangle mesh found")
 return out

def check_geometry(meshes,target,tolerance):
 verts=np.concatenate([g.vertices for _,g in meshes if len(g.vertices)])
 mn=verts.min(axis=0);mx=verts.max(axis=0);dim=mx-mn
 actual={"span_x_m":float(dim[0]),"height_y_m":float(dim[1]),"length_z_m":float(dim[2])}
 errors={k:actual[k]-v for k,v in EXPECTED.items()}
 parts=[{"part":n,"vertices":len(m.vertices),"triangles":len(m.faces),"watertight":bool(m.is_watertight)} for n,m in meshes]
 report={
  "phase":"GEOMETRIC_PREFLIGHT_ONLY","axes":"X span, Y up, +Z forward","units":"metres",
  "expected":EXPECTED,"actual":actual,"delta_m":errors,
  "bbox_min":mn.tolist(),"bbox_max":mx.tolist(),
  "dimension_pass":all(abs(v)<=tolerance for v in errors.values()),
  "tolerance_m":tolerance,"part_count":len(parts),"triangles":sum(x["triangles"] for x in parts),
  "visual_approval":"PENDING_USER_REVIEW","parts":parts
 }
 target.write_text(json.dumps(report,indent=2,ensure_ascii=False))
 return report

def render(meshes,output,width=1500,height=1000):
 import vtk
 merged=vtk.vtkAppendPolyData()
 for _,m in meshes:
  if not len(m.faces):continue
  points=vtk.vtkPoints();points.SetDataTypeToDouble()
  for xyz in m.vertices:points.InsertNextPoint(*map(float,xyz))
  faces=vtk.vtkCellArray()
  for f in m.faces:
   t=vtk.vtkTriangle()
   for j,idx in enumerate(f):t.GetPointIds().SetId(j,int(idx))
   faces.InsertNextCell(t)
  pd=vtk.vtkPolyData();pd.SetPoints(points);pd.SetPolys(faces);merged.AddInputData(pd)
 merged.Update()
 mapper=vtk.vtkPolyDataMapper();mapper.SetInputConnection(merged.GetOutputPort())
 actor=vtk.vtkActor();actor.SetMapper(mapper)
 actor.GetProperty().SetColor(.85,.88,.91);actor.GetProperty().SetSpecular(.18)
 actor.GetProperty().SetAmbient(.35);actor.GetProperty().SetDiffuse(.65)
 ren=vtk.vtkRenderer();ren.SetBackground(.095,.125,.16);ren.AddActor(actor)
 win=vtk.vtkRenderWindow();win.SetOffScreenRendering(1);win.AddRenderer(ren);win.SetSize(width,height)
 b=merged.GetOutput().GetBounds()
 center=np.array([(b[i]+b[i+1])/2 for i in (0,2,4)])
 corners=np.array([[x,y,z] for x in (b[0],b[1]) for y in (b[2],b[3]) for z in (b[4],b[5])])-center
 for name,(offset,up) in VIEWS.items():
  cam=ren.GetActiveCamera()
  cam.SetFocalPoint(*center);cam.SetPosition(*(center+np.asarray(offset)));cam.SetViewUp(*up)
  cam.ParallelProjectionOn();cam.OrthogonalizeViewUp()
  forward=center-np.asarray(cam.GetPosition());forward/=np.linalg.norm(forward)
  right=np.cross(forward,np.asarray(cam.GetViewUp()));right/=np.linalg.norm(right)
  upvec=np.cross(right,forward)
  h=np.abs(corners@right).max()*2;v=np.abs(corners@upvec).max()*2
  cam.SetParallelScale(max(v/2,h/(2*width/height))*1.12)
  ren.ResetCameraClippingRange();win.Render()
  w2i=vtk.vtkWindowToImageFilter();w2i.SetInput(win)
  writer=vtk.vtkPNGWriter();writer.SetFileName(str(output/(name+".png")))
  writer.SetInputConnection(w2i.GetOutputPort());writer.Write()
 win.Finalize()

def main():
 p=argparse.ArgumentParser()
 p.add_argument("model",type=Path)
 p.add_argument("--output",type=Path,default=Path("review"))
 p.add_argument("--tolerance",type=float,default=.12)
 p.add_argument("--skip-render",action="store_true")
 a=p.parse_args();a.output.mkdir(exist_ok=True,parents=True)
 meshes=read_model(a.model)
 report=check_geometry(meshes,a.output/"geometry_report.json",a.tolerance)
 print(json.dumps({k:v for k,v in report.items() if k!="parts"},indent=2))
 if not a.skip_render:
  render(meshes,a.output)
  print("Wrote six consistent 3D preview PNGs to",a.output)

if __name__=="__main__":main()
