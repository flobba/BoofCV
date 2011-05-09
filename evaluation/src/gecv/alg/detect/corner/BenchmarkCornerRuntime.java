/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperFastCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMaxCandidate;
import gecv.alg.detect.extract.NonMaxCornerCandidateExtractor;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * Benchmark which scores the whole corner detection processing stack.
 *
 * @author Peter Abeles
 */
public class BenchmarkCornerRuntime {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static long TEST_TIME = 1000;

	static ImageFloat32 image_F32;
	static ImageFloat32 derivX_F32;
	static ImageFloat32 derivY_F32;
	static ImageInt8 image_I8;
	static ImageInt16 derivX_I16;
	static ImageInt16 derivY_I16;

	public static class Detector_F32 extends PerformerBase {
		GeneralCornerDetector<ImageFloat32,ImageFloat32> alg;

		public Detector_F32(GeneralCornerDetector<ImageFloat32,ImageFloat32> alg) {
			this.alg = alg;
		}

		@Override
		public void process() {
			alg.process(image_F32,derivX_F32, derivY_F32);
		}
	}

	public static void benchmark(GeneralCornerDetector<ImageFloat32,ImageFloat32> alg, String name) {
		double opsPerSec = ProfileOperation.profileOpsPerSec(new Detector_F32(alg), TEST_TIME);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}


	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createFast12_F32() {
		FastCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createFast12_F32(imgWidth, imgHeight, 30, 12);

		return createFastDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createHarris_F32() {
		HarrisCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createHarris_F32(imgWidth, imgHeight, windowRadius, 0.04f);

		return createGradientDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createKitRos_F32() {
		KitRosCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKitRos_F32(imgWidth, imgHeight, windowRadius);

		return createGradientDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createKlt_F32() {
		KltCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKlt_F32(imgWidth, imgHeight, windowRadius);

		return createGradientDetector(alg);
	}

	private static GeneralCornerDetector<ImageFloat32,ImageFloat32> createGradientDetector(GradientCornerIntensity<ImageFloat32> alg) {
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity = new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(alg);
		CornerExtractor extractorCandidate = new WrapperNonMaxCandidate(new NonMaxCornerCandidateExtractor(windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity, extractorCandidate, imgWidth * imgHeight / (windowRadius * windowRadius));
	}

	private static GeneralCornerDetector<ImageFloat32,ImageFloat32> createFastDetector(FastCornerIntensity<ImageFloat32> alg) {
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity = new WrapperFastCornerIntensity<ImageFloat32,ImageFloat32>(alg);
		CornerExtractor extractorCandidate = new WrapperNonMaxCandidate(new NonMaxCornerCandidateExtractor(windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity, extractorCandidate, imgWidth * imgHeight / (windowRadius * windowRadius));
	}

	public static void main(String args[]) {

		derivX_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivX_I16 = new ImageInt16(imgWidth, imgHeight, true);
		derivY_I16 = new ImageInt16(imgWidth, imgHeight, true);

		FileImageSequence sequence = new FileImageSequence("data/indoors01.jpg", "data/outdoors01.jpg", "data/particles01.jpg");

		while (sequence.next()) {
			image_I8 = sequence.getImage_I8();
			image_F32 = sequence.getImage_F32();

			imgWidth = image_I8.getWidth();
			imgHeight = image_I8.getHeight();

			System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
			System.out.println("           " + sequence.getName());
			System.out.println();

			derivX_F32.reshape(imgWidth, imgHeight);
			derivY_F32.reshape(imgWidth, imgHeight);
			derivX_I16.reshape(imgWidth, imgHeight);
			derivY_I16.reshape(imgWidth, imgHeight);

			GradientSobel.process_F32(image_F32, derivX_F32, derivY_F32);
			GradientSobel.process_I8(image_I8, derivX_I16, derivY_I16);


			benchmark(createKlt_F32(), "KLT F32");
			benchmark(createFast12_F32(), "Fast F32");
			benchmark(createHarris_F32(), "Harris F32");
			benchmark(createKitRos_F32(), "Kit Ros F32");

			System.out.println();
		}
	}
}
