package com.lewa.support.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

class MMCQ {
	public int[] domaincolor =null;
	public PQueue pq2 = null;
	private static String TAG = "MMCQ";
	private int  sigbits = 5;
	private int rshift = 8 - sigbits;
	private int maxIterations = 1000;
	private double fractByPopulations = 0.75;
	private int  total = 0;
    private int[] partialsum;
    private int[] lookaheadsum;
	
	public MMCQ() {
		
	}
	
	public void GetBitmapMMCQ(Bitmap bitmap,int colorCount,int quality)
    {
        if(colorCount == 0){
    		colorCount = 10;
    	}
    	
    	if(quality == 0){
    		quality = 10;
    	}

	pq2 = null;
    	int width = bitmap.getWidth();
    	int height = bitmap.getHeight();
    	int pixelCount = width * height;
    	int[] pixels = new int[pixelCount];
    	
    	bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    	ArrayList<RgbModel> pixelArray = new ArrayList<RgbModel>();
    	RgbModel rgb = null;
    	int offset, r, g, b, a,pixel;
    	for(int i=0;i<pixelCount;i=i+quality){
    	    pixel = pixels[i];
    		r = Color.red(pixels[i]);
            g =Color.green(pixels[i]);
            b = Color.blue(pixels[i]);
            a = Color.alpha(pixels[i]);
            
            // If pixel is mostly opaque and not white
            if (a >= 125) {
                if (!(r > 250 && g > 250 && b > 250)) {
                	rgb = new RgbModel();
                	rgb.setR(r);
                	rgb.setG(g);
                	rgb.setB(b);
                    pixelArray.add(rgb);
                }
            }
    	}
    	
    	quantize(pixelArray, colorCount);
    }
	
	private void  quantize(ArrayList<RgbModel> pixels,int  maxcolors) {
        if (pixels.size() == 0 || maxcolors < 2 || maxcolors > 256) {
            return ;
        }

        int[] histo = getHisto(pixels);
        int nColors =0;
        for(int i=0;i<histo.length;i++){
        	if(histo[i] !=0){
        		nColors = nColors +1;
        	}
        }
        
        if (nColors <= maxcolors) {
        	//// XXX: generate the new colors from the histo and return
        }

        // get the beginning vbox from the colors
        VBox vbox = vboxFromPixels(pixels, histo);
        
        PQueue pq = new PQueue(new Comparator<VBox>(){
        	@Override
            public int compare(VBox a , VBox b){
               return (a.count() < b.count()) ? -1 : ((a.count() > b.count()) ? 1 : 0 );
            };
        });
        
        pq.push(vbox);
        iter(pq, fractByPopulations * maxcolors,histo,vbox);
        
        pq2 = new PQueue(new Comparator<VBox>(){
        	@Override
            public int compare(VBox a , VBox b){
               return (a.count()*a.volume() < b.count()*b.volume()) ? -1 : ((a.count()*a.volume() > b.count()*b.volume()) ? 1 : 0 );
            };
        });
        
        while (pq.size() !=0) {
            pq2.push(pq.pop());
        }

        iter(pq2, maxcolors - pq2.size(),histo,vbox);
    	
    }

	private void iter(PQueue lh,double  target,int[] histo,VBox vbox) {
	    int ncolors = 1;
	    int niters = 0;
	
	    while (niters < maxIterations) {
	        vbox = lh.pop();
	        if ( vbox.count() ==0)  {
	            lh.push(vbox);
	            niters++;
	            continue;
	        }
	        
	        VBox[] vboxes = medianCutApply(histo, vbox);
	        VBox vbox1 = vboxes[0];
	        VBox vbox2 = vboxes[1];
	
	        if (vbox1 == null) {
	            return;
	        }
	        
	        lh.push(vbox1);
	        if (vbox2 != null) {
	        	lh.push(vbox2);
	            ncolors++;
	        }
	        
	        if (ncolors >= target) {
	        	return;
	        }
	        
	        if (niters++ > maxIterations) {
	            return;
	        }
	    }
	}
			
	public int[]  getHisto(ArrayList<RgbModel> pixels) {
	     int  histosize = 1 << (3 * sigbits);
	     int[] histo = new int[histosize];
	     int  index, rval, gval, bval;
	     
	     for(RgbModel rgb:pixels){
	    	 rval = rgb.getR()>>rshift;
	     	 gval = rgb.getG()>>rshift;
		     bval = rgb.getB()>>rshift;
        
		     index = getColorIndex(rval, gval, bval);
	         histo[index] = histo[index] + 1;

	     }
	 
	     return histo;
	}

	private  int getColorIndex(int r,int  g,int  b) {
	    return (r << (2 * sigbits)) + (g << sigbits) + b;
	}

	private  VBox vboxFromPixels(ArrayList<RgbModel>pixels,int[] histo) {
        int  rmin=1000000;
        int rmax=0;
        int gmin=1000000;
        int gmax=0;
        int bmin=1000000;
        int bmax=0;
        int rval = 0;
        int gval = 0;
        int bval = 0;

        for(RgbModel pixel :pixels ){
        	rval = pixel.getR() >> rshift;
            gval = pixel.getG()>> rshift;
            bval = pixel.getB() >> rshift;
            
            if (rval < rmin) {
            	rmin = rval;
            }else if (rval > rmax){
            	rmax = rval;
            }
            if (gval < gmin) {
            	gmin = gval;
            }else if (gval > gmax) {
            	gmax = gval;
            }
            
            if (bval < bmin) {
            	bmin = bval;
            }else if (bval > bmax) {
            	bmax = bval;
            }
        }
        
        VBox vbox = new VBox();
        vbox.setR1(rmin);
        vbox.setR2(rmax);
        vbox.setG1(gmin);
        vbox.setG2(gmax);
        vbox.setB1(bmin);
        vbox.setB2(bmax);
        vbox.setHisto(histo);
       
        return vbox;
    }

	private VBox[] medianCutApply(int[] histo, VBox vbox) {
		   VBox[] mVBox = null;
		   if(vbox.count() ==0){
			   return null;
		   }

	        int  rw = vbox.getR2() - vbox.getR1() + 1;
	        int gw = vbox.getG2() - vbox.getG1() + 1;
	        int bw = vbox.getB2() - vbox.getB1() + 1;
	        int maxw = getMax(rw, gw, bw);
	    	
	        if (vbox.count() == 1) {
	        	mVBox = new VBox[1];
	        	mVBox[0] = vbox;
	            return mVBox;
	        }
	        
	        int  sum =0;
	        int index = 0;
	        total = 0;
	        if (maxw == rw) {
	        	partialsum = new int[rw];
	            for (int i = vbox.r1; i <= vbox.r2; i++) {
	                sum = 0;
	                for (int j = vbox.g1; j <= vbox.g2; j++) {
	                    for (int k = vbox.b1; k <= vbox.b2; k++) {
	                        index = getColorIndex(i,j,k);
	                        sum +=histo[index];
	                    }
	                }
	                total += sum;
	                partialsum[i-vbox.r1] = total;
	            }
	        }else if (maxw == gw) {
	        	partialsum = new int[gw];
	            for (int i = vbox.g1; i <= vbox.g2; i++) {
	                sum = 0;
	                for (int j = vbox.r1; j <= vbox.r2; j++) {
	                    for (int k = vbox.b1; k <= vbox.b2; k++) {
	                        index = getColorIndex(j,i,k);
	                        sum +=histo[index];
	                    }
	                }
	                total += sum;
	                partialsum[i-vbox.g1] = total;
	            }
	        }else { 
	        	partialsum = new int[bw];
	            for (int i = vbox.b1; i <= vbox.b2; i++) {
	                sum = 0;
	                for (int j = vbox.r1; j <= vbox.r2; j++) {
	                    for (int k = vbox.g1; k <= vbox.g2; k++) {
	                        index = getColorIndex(j,k,i);
	                        sum +=histo[index];
	                    }
	                }
	                total += sum;
	                partialsum[i-vbox.b1] = total;
	            }
	        }
	        
	        lookaheadsum = new int[partialsum.length];
	        for(int i=0;i<partialsum.length;i++){
	        	lookaheadsum[i] = total - partialsum[i];
	        }
	        
	        if (maxw == rw) {
	        	mVBox = doCut("r",vbox);
	        }else if (maxw == gw) {
	        	mVBox = doCut("g",vbox);
	        }else { 
	        	mVBox = doCut("b",vbox);
	        }
	        
	        return mVBox;
	}
	
	private  VBox[] doCut(String color,VBox vbox) {
		VBox[] mVBox = new VBox[2];
		int left = 0;
        int right = 0;
        VBox vbox1 = null;
        VBox vbox2 = null;
        int d2 = 0;
        int count2=0;
        
		int rgbMin = 0;
		int rgbMax = 0;
		
		if("r".equals(color)){
			rgbMin = vbox.getR1();
			rgbMax = vbox.getR2();
			
		}else if("g".equals(color)){
			rgbMin = vbox.getG1();
			rgbMax = vbox.getG2();
			
		}else if("b".equals(color)){
			rgbMin = vbox.getB1();
			rgbMax = vbox.getB2();
			
		}
		
        for (int i = rgbMin; i <= rgbMax; i++) {
            if (partialsum[i-rgbMin] > total / 2) {
                vbox1 = (VBox)vbox.clone();
                vbox2 =  (VBox)vbox.clone();
                left = i - rgbMin;
                right = rgbMax - i;
                
                if (left <= right){
                    d2 = Math.min(rgbMax- 1, (i + right / 2));
                }else{
                	d2 = Math.max(rgbMin,(i - 1 - left / 2));
                }

		  if(d2<0){
		  	d2 = 0;
		  }

                while(  d2<partialsum.length && partialsum[d2] ==0){
                	d2++;
                }
                
                if(d2<partialsum.length){
                	count2 = lookaheadsum[d2];
                }
                
                while(count2 ==0 && (d2-1)<partialsum.length && (d2-1)>=0 && partialsum[d2-1] !=0 ){
                	count2 = lookaheadsum[--d2];
                }
                
                if("r".equals(color)){
                	vbox1.setR2(d2);
                	vbox2.setR1(vbox1.getR2() +1);
    			}else if("g".equals(color)){
    				vbox1.setG2(d2);
    				vbox2.setG1(vbox1.getG2() +1);
    			}else if("b".equals(color)){
    				vbox1.setB2(d2);
    				vbox2.setB1(vbox1.getB2() +1);
    			}
                
                mVBox[0] = vbox1;
                mVBox[1] = vbox2;
                return mVBox;
            }
        }
        return null;
    }
	

	private int getMax(int rw,int gw,int bw){
		int maxv = 0;
		if(rw >=gw){
			maxv = rw;
		}else{
			maxv = gw;
		}
		
		if(maxv <bw){
			maxv = bw;
		}
		
		return maxv;
	}

	public class RgbModel{
		int r =0;
		int g = 0;
		int b = 0;
		
		public int getR() {
			return r;
		}
		public void setR(int r) {
			this.r = r;
		}
		public int getG() {
			return g;
		}
		public void setG(int g) {
			this.g = g;
		}
		public int getB() {
			return b;
		}
		public void setB(int b) {
			this.b = b;
		}
	}
	
	public class PQueue{
		   private Comparator<VBox> comp;
		   private Stack<VBox> contents = new Stack<VBox>();
		   private boolean sort;
		   
		   public PQueue(Comparator<VBox> comp){
			      this.comp = comp;
			      this.sort = false;
		   }

		   public void sort(){
		       Collections.sort(contents , this.comp);
		   }

		   public void push(VBox box){
			   contents.push(box);
		       this.sort = false;
		   }

		   public VBox pop(){
		      if(!sort) sort();

		      if(!contents.isEmpty()){
		    	  return contents.pop();
		      } 
		      return null;
		   }
		   
		   public int size(){
			   return contents.size();
		   }
		   
		   public VBox peek(int index){
			   if(!sort) sort();
			   if(index == 0){
				   index = contents.size()-1;
			   }
			   
			   return contents.get(index);
		   }
	}
	
	public class CMap{
		private int[] color;
		private VBox vbox = null;
		private PQueue vboxes = null;
		
		public CMap(){
			   vboxes = new PQueue(new Comparator<VBox>(){
	        	@Override
	            public int compare(VBox a , VBox b){
	               return (a.count()*a.volume() < b.count()*b.volume()) ? -1 : ((a.count()*a.volume() > b.count()*b.volume()) ? 1 : 0 );
	            };
	        }); 
		}
		
		public void push(VBox vbox){
			vboxes.push(vbox);
		}
		
		public int size(){
			return vboxes.size();
		}
		
		public void palette(){
			
		}
		
		public int[] map(int[] color){
			for(int i=0;i<vboxes.size();i++){
				int[] mavg = vboxes.peek(i).avg();
				
				if(mavg == color){
					return mavg;
				}
			}
			
			return nearest(color);
		}
		
		public int[] nearest(int[] color){
			double d1 = 0;
			double d2 = 0;
			int[] pColor = {};
			for(int i=0;i<vboxes.size();i++){
				int[] mavg = vboxes.peek(i).avg();
				d2 = Math.sqrt(
							Math.pow(color[0]-mavg[0],2) + 
							Math.pow(color[1]-mavg[1],2) + 
							Math.pow(color[2]-mavg[2],2) 
						);
				if(d2<d1){
					d1 = d2;
				}
				
				pColor = mavg;
			}
			
			return pColor;
		}
		
		public VBox getVbox() {
			return vbox;
		}
		public void setVbox(VBox vbox) {
			this.vbox = vbox;
		}
		public int[] getColor() {
			return color;
		}
		public void setColor(int[] color) {
			this.color = color;
		}
	}
	
	public class VBox implements Cloneable{
		private int r1;	
		private int r2;
		private int g1;
		private int g2;
		private int b1;
		private int b2;
		private int[] histo;
		private int mVolume = 0;
		private int mCount = 0;
		private boolean mCountSet = false;
		private int[] mAvg = new int[3];
		
		public Object clone() {  
			VBox vbox = null;  
	        try {  
	        	vbox = (VBox) super.clone();  
	        } catch (CloneNotSupportedException e) {  
	            e.printStackTrace();  
	        }  
	        return vbox;  
	    }  
		
		public int volume(){
			mVolume = (r2 - r1 + 1) * (g2 - g1 + 1) * (b2 - b1 + 1);
			return mVolume;
		}
		
		public int count(){
			int  npix = 0;
			int index = 0;
			for (int i = r1; i <= r2; i++) {
                for (int j = g1; j <= g2; j++) {
                    for (int k = b1; k <= b2; k++) {
                         index = getColorIndex(i,j,k);
                         npix += histo[index];
                    }
                }
            }
			mCount = npix;
			mCountSet = true;
			
			return mCount;
		}
		
		public int[] avg(){
			int  ntot = 0;
            int mult = 1 << (8 - sigbits);
            int rsum = 0;
            int gsum = 0;
            int bsum = 0;
            int hval=0;
            int histoindex;
            
            for (int i = r1; i <= r2; i++) {
                 for (int j = g1; j <= g2; j++) {
                      for (int k = b1; k <= b2; k++) {
                             histoindex = getColorIndex(i,j,k);
                             hval = histo[histoindex] ;
                             ntot += hval;
                             rsum += (hval * (i + 0.5) * mult);
                             gsum += (hval * (j + 0.5) * mult);
                             bsum += (hval * (k + 0.5) * mult);
                        }
                    }
                }
                if (ntot ==0) {
                	mAvg[0] = (int)(r1 + r2 + 1) / 2;
                	mAvg[1] = (int)(g1 + g2 + 1) / 2;
                	mAvg[2] = (int)(b1 + b2 + 1) / 2;
                } else {
                	mAvg[0] = (int)rsum/ntot;
                	mAvg[1] = (int)gsum/ntot;
                	mAvg[2] = (int)bsum/ntot;
                }
			return mAvg;
		}
		
		
		public int getR1() {
			return r1;
		}
		public void setR1(int r1) {
			this.r1 = r1;
		}
		public int getR2() {
			return r2;
		}
		public void setR2(int r2) {
			this.r2 = r2;
		}
		public int getG1() {
			return g1;
		}
		public void setG1(int g1) {
			this.g1 = g1;
		}
		public int getG2() {
			return g2;
		}
		public void setG2(int g2) {
			this.g2 = g2;
		}
		public int getB1() {
			return b1;
		}
		public void setB1(int b1) {
			this.b1 = b1;
		}
		public int getB2() {
			return b2;
		}
		public void setB2(int b2) {
			this.b2 = b2;
		}
		public int[] getHisto() {
			return histo;
		}
		public void setHisto(int[] histo) {
			this.histo = histo;
		}
	}
}
